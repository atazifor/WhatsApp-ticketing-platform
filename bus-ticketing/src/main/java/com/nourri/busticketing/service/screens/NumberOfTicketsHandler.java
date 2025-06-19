package com.nourri.busticketing.service.screens;

import com.nourri.busticketing.dto.FlowDataExchangePayload;
import com.nourri.busticketing.dto.NextScreenResponsePayload;
import com.nourri.busticketing.dto.ScreenHandlerResult;
import com.nourri.busticketing.model.Schedule;
import com.nourri.busticketing.model.Seat;
import com.nourri.busticketing.dto.BookingState;
import com.nourri.busticketing.repository.SeatRepository;
import com.nourri.busticketing.service.AgencyService;
import com.nourri.busticketing.service.ImageOverlayService;
import com.nourri.busticketing.service.ScheduleService;
import com.nourri.busticketing.whatsapp.session.SessionContextStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.nourri.busticketing.service.Screen.*;

@Component("NUMBER_OF_TICKETS")
@RequiredArgsConstructor
public class NumberOfTicketsHandler implements ScreenHandler {
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NumberOfTicketsHandler.class);

    private final SessionContextStore sessionContextStore;
    private final ScheduleService scheduleService;
    private final SeatRepository seatRepository;
    private final ImageOverlayService imageOverlayService;
    private final AgencyService agencyService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {

        String numberOfTickets = payload.getData().get("number_of_tickets").toString();

        BookingState newState = state.withNumTickets(numberOfTickets);

        //get available seats from agency metadata service
        Optional<Schedule> optDetails = scheduleService.findScheduleDetails(
            state.getAgency(),
            state.getOrigin(),
            state.getDestination(),
            state.getTravelClass(),
            state.getTime(),
            state.getDate()
        );

        NextScreenResponsePayload nextScreenResponsePayload;
        if (optDetails.isPresent()) {
            Schedule schedule = optDetails.get();
            boolean openSeating = schedule.getBus().getIsOpenSeating();
            if(openSeating) {
                newState = newState.withStep(STEP_PASSENGER_INFORMATION);
                nextScreenResponsePayload = initPassengerInfoUi(payload, newState);
            }else {
                // Fetch available seats for that class and schedule
                List<Seat> unsoldSeats = seatRepository.findAvailableSeatsByScheduleAndClass(schedule, state.getTravelClass());
                List<String> availableSeatNumbers = unsoldSeats.stream()
                    .map(Seat::getSeatNumber)
                    .toList();

                newState = newState.withStep(STEP_CHOOSE_SEAT);
                nextScreenResponsePayload = initChooseSeatUi(newState, availableSeatNumbers);
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

        String busBase64 = imageOverlayService.createAvailabilityOverlay(new HashSet<>(availableSeats));

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

        if (seats.size() == 1) { //for display purposes
            seats.add(Map.of(
                "id", "disabled_option",
                "title", "Only Option",
                "enabled", true
            ));
        }

        logger.debug("Seats to display size: {}", seats.size());

        int maxPerBooking = agencyService.getMaxTicketsPerBooking(state.getAgency());
        int requestedCount = Integer.parseInt(state.getNumTickets());
        int maxSelectable = Math.min(requestedCount, Math.min(maxPerBooking, availableSeats.size()));
        logger.debug("Max selectable: {}", maxSelectable);
        //Put into a single responseData map
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("image", busBase64);
        fields.put("seats", seats);
        fields.put("maxSelectable", maxSelectable);
        fields.put("current_passenger_index", requestedCount > 0 ? "1" : "");

        // Return payload whose `responseData` matches your JSON layout’s placeholders
        return new NextScreenResponsePayload(STEP_CHOOSE_SEAT, fields);
    }
}
