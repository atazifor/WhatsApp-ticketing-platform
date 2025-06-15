package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.model.ScheduleDetails;
import com.tazifor.busticketing.model.SeatingInfo;
import com.tazifor.busticketing.service.AgencyMetadataService;
import com.tazifor.busticketing.service.ScheduleQueryService;
import com.tazifor.busticketing.util.ImageOverlayUtil;
import com.tazifor.busticketing.whatsapp.session.SessionContextStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.tazifor.busticketing.service.Screen.*;

@Component("NUMBER_OF_TICKETS")
@RequiredArgsConstructor
public class NumberOfTicketsHandler implements ScreenHandler {
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NumberOfTicketsHandler.class);

    private final SessionContextStore sessionContextStore;
    private final ScheduleQueryService scheduleQueryService;
    private final ImageOverlayUtil imageOverlayUtil;
    private final AgencyMetadataService agencyMetadataService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {

        String numberOfTickets = payload.getData().get("number_of_tickets").toString();

        BookingState newState = state.withNumTickets(numberOfTickets);

        //get available seats from agency metadata service
        Optional<ScheduleDetails> optDetails = scheduleQueryService.findScheduleDetails(
            state.getAgency(),
            state.getOrigin(),
            state.getDestination(),
            state.getTravelClass(),
            state.getTime()
        );

        NextScreenResponsePayload nextScreenResponsePayload;
        if (optDetails.isPresent()) {
            SeatingInfo seatingInfo = optDetails.get().seatingInfo();
            boolean openSeating = seatingInfo.isOpenSeating();
            if(openSeating) {
                newState = newState.withStep(STEP_PASSENGER_INFORMATION);
                nextScreenResponsePayload = initPassengerInfoUi(payload, newState);
            }else {
                List<String> availableSeats = seatingInfo.getAllAvailableSeatNumbers(state.getTravelClass());

                newState = newState.withStep(STEP_CHOOSE_SEAT);
                nextScreenResponsePayload = initChooseSeatUi(newState, availableSeats);
            }

        }else { //TODO: why would optDetails be empty?
            newState = newState.withStep(STEP_PASSENGER_INFORMATION);
            nextScreenResponsePayload = initPassengerInfoUi(payload, newState);
        }

        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

    private NextScreenResponsePayload initPassengerInfoUi(FlowDataExchangePayload payload, BookingState state) {
        String userWhatsAppPhoneNumber = sessionContextStore.getUserPhone(payload.getFlow_token()).orElse("");
        return new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION,
            Map.of(
                "user_wa_number", userWhatsAppPhoneNumber,
                "current_passenger_index", Integer.parseInt(state.getNumTickets()) > 0 ? "1" : ""
            )
        );
    }

    private NextScreenResponsePayload initChooseSeatUi(BookingState state, List<String> availableSeats) {
        logger.debug("Available seats: {}", availableSeats);

        String busBase64 = imageOverlayUtil.createAvailabilityOverlay(new HashSet<>(availableSeats)); // “responseData:image/png;base64,…”

        if (busBase64 == null) {
            // If for some reason the service didn’t produce an image or any seats
            Map<String,Object> err = Map.of(
                "error_message", "🚧 Unable to load seat map right now."
            );
            return new NextScreenResponsePayload(STEP_CHOOSE_SEAT, err);
        }

        List<Map<String,Object>> seats = availableSeats.stream()
            .sorted()
            .map(seatId -> Map.<String,Object>of(
                "id", seatId,
                "title", seatId,
                // on-select-action must have an empty payload so Flow doesn’t reject
                "on-select-action", Map.of(
                    "name",    "update_data",
                    "enabled", true,
                    "payload", Map.of()  // payload is intentionally empty
                )
            ))
            .collect(Collectors.toList());

        int maxTicketsPerBooking = agencyMetadataService.getConfig(state.getAgency()).maxTicketsPerBooking();
        maxTicketsPerBooking = Math.min(maxTicketsPerBooking, availableSeats.size());
        int numberOfTickets = state.getNumTickets().isEmpty() ? 0 : Integer.parseInt(state.getNumTickets());
        maxTicketsPerBooking = Math.min(maxTicketsPerBooking, numberOfTickets);

        //Put into a single responseData map
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("image", busBase64);
        fields.put("seats", seats);
        fields.put("maxSelectable", maxTicketsPerBooking);
        fields.put("current_passenger_index", Integer.parseInt(state.getNumTickets()) > 0 ? "1" : "");

        // Return payload whose `responseData` matches your JSON layout’s placeholders
        return new NextScreenResponsePayload(STEP_CHOOSE_SEAT, fields);
    }
}
