package com.nourri.busticketing.service.screens;

import com.nourri.busticketing.dto.FlowDataExchangePayload;
import com.nourri.busticketing.dto.NextScreenResponsePayload;
import com.nourri.busticketing.dto.ScreenHandlerResult;
import com.nourri.busticketing.model.AgencyContact;
import com.nourri.busticketing.dto.BookingState;
import com.nourri.busticketing.service.AgencyService;
import com.nourri.busticketing.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static com.nourri.busticketing.service.Screen.STEP_NUMBER_OF_TICKETS;
import static org.springframework.util.StringUtils.capitalize;

@Component("DISPLAY_RESULTS")
@RequiredArgsConstructor
public class DisplayResultsHandler implements ScreenHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(DisplayResultsHandler.class);

    private final AgencyService agencyService;
    private final ScheduleService scheduleService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        // 2) Advance our state machine to CHOOSE_SEAT
        Map<String, Object> data = payload.getData();

        BookingState newState = state.withTime(data.get("time").toString())
            .withTravelClass(data.get("class").toString())
            .withAgency(data.get("agency").toString())
            .withPrice(data.get("price").toString())
            .withStep(STEP_NUMBER_OF_TICKETS);

        int maxTicketsPerBooking = agencyService.getMaxTicketsPerBooking(newState.getAgency());
        /*int available = seatRepository.countByScheduleIdAndTravelClassIdAndIsSoldFalse(
            schedule.getId(),
            travelClass.getId()
        );*/
        int unsold = scheduleService.getUnsoldSeats(
            newState.getAgency(),
            newState.getOrigin(),
            newState.getDestination(),
            newState.getTravelClass(),
            newState.getDate(),
            newState.getTime()
        );

        LOGGER.debug("ScreenHandlerResult.Unsold seats: {}", unsold);

        int numTicketsForDropdown = Math.min(unsold, maxTicketsPerBooking);

        List<Map<String, String>> numberOfTicketsUi = IntStream.rangeClosed(1, numTicketsForDropdown)
            .mapToObj(i ->
                Map.of(
                    "id", String.valueOf(i),
                    "title", i + " Ticket" + (i > 1 ? "s" : "")
                )
            )
            .toList();

        Optional<AgencyContact> contactOpt = agencyService.getContact(newState.getAgency(), newState.getOrigin());

        String thresholdText = getTicketsThresholdText(newState.getAgency(), maxTicketsPerBooking, contactOpt);

        // 4b) Put into a single responseData map
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("number_of_tickets", numberOfTicketsUi);
        fields.put("tickets_threshold_text", thresholdText);


        // 5) Return payload whose `responseData` matches your JSON layout’s placeholders
        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_NUMBER_OF_TICKETS, fields);
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

    public String getTicketsThresholdText(String agencyName, int maxPerBooking, Optional<AgencyContact> contactOpt) {
        if (contactOpt.isPresent()) {
            AgencyContact contact = contactOpt.get();
            return String.format(
                "For more than **%d** tickets, please contact **%s** at **%s**.\nLocation: _%s, %s_",
                maxPerBooking,
                agencyName,
                contact.getPhone(),
                contact.getAddress(),
                capitalize(contact.getCity())
            );
        } else {
            return String.format(
                "For more than **%d** tickets, please contact **%s** directly.",
                maxPerBooking,
                agencyName
            );
        }
    }
}
