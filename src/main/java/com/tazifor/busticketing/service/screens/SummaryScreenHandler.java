package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.*;
import com.tazifor.busticketing.exception.BusinessException;
import com.tazifor.busticketing.model.AgencyContact;
import com.tazifor.busticketing.dto.BookingState;
import com.tazifor.busticketing.dto.Passenger;
import com.tazifor.busticketing.model.Schedule;
import com.tazifor.busticketing.service.AgencyService;
import com.tazifor.busticketing.service.ScheduleService;
import com.tazifor.busticketing.util.BookingFormatter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.tazifor.busticketing.service.Screen.*;

@Component("SUMMARY")
@RequiredArgsConstructor
public class SummaryScreenHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(SummaryScreenHandler.class);
    private final AgencyService agencyService;
    private final ScheduleService scheduleService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        //Map<String, Object> data = payload.getData();
        String origin = state.getOrigin();
        String destination = state.getDestination();
        String date = state.getDate();
        String isRoundTrip = state.isRoundTrip() ? "true" : "false";
        String time = state.getTime();
        String travelClass = state.getTravelClass();
        String moreDetails = state.getMoreDetails() != null ? state.getMoreDetails() : "";
        Passenger booker = state.getPassengerList().get(0);

        // Check if the user agreed to terms
        boolean agreed = Boolean.parseBoolean(
            payload.getData().getOrDefault("agree_terms", "false").toString()
        );

        if (!agreed) {
            logger.debug("User did not agree to terms. Returning to STEP_SUMMARY");
            // Re‐show SUMMARY with an error
            Map<String, Object> errorData = new LinkedHashMap<>();
            errorData.put("appointment",    BookingFormatter.formatAppointment(origin, destination, date, time));
            errorData.put("details", BookingFormatter.formatDetails(booker.getName(), booker.getEmail(), booker.getPhone(), moreDetails));
            errorData.put("error_message",  "❗ You must agree to the terms to proceed.");
            return new ScreenHandlerResult(state.withStep(STEP_SUMMARY), new NextScreenResponsePayload(STEP_SUMMARY, errorData));
        }
        BookingState newState = state.withStep("END");

        logger.debug("User agreed to terms. Returning to END witg agency {} and origin {}",
            state.getAgency(), state.getOrigin());

        AgencyContact agencyContact = agencyService.getContact(state.getAgency(), state.getOrigin())
            .orElseThrow(() -> new BusinessException("No contact found for agency " + state.getAgency() + " in city " + state.getOrigin()));

        Schedule scheduleDetails = scheduleService.findScheduleDetails(state.getAgency(), state.getOrigin(),
            state.getDestination(), state.getTravelClass(),
            state.getTime(), state.getDate())
            .orElseThrow(() -> new BusinessException("No schedule found for the specified criteria"));

        // Otherwise, finalize booking
        Map<String, Object> finalParams = new LinkedHashMap<>();
        finalParams.put("schedule_id",scheduleDetails.getId());
        finalParams.put("origin", origin);
        finalParams.put("destination", destination);
        finalParams.put("date", date);
        finalParams.put("is_round_trip", isRoundTrip);
        finalParams.put("time", time);
        finalParams.put("class", travelClass);
        finalParams.put("agency", state.getAgency());
        finalParams.put("agency_phone", agencyContact.getPhone());
        finalParams.put("agency_address", agencyContact.getAddress());
        finalParams.put("price", state.getPrice());
        finalParams.put("seat", state.getChosenSeats());
        finalParams.put("passengers", state.getPassengerList());
        finalParams.put("num_tickets", state.getNumTickets());
        finalParams.put("more_details", moreDetails);
        finalParams.put("flow_token", payload.getFlow_token());

        var extMsgResponse = new FinalScreenResponsePayload.ExtensionMessageResponse(finalParams);
        FinalScreenResponsePayload finalScreenResponsePayload = new FinalScreenResponsePayload(extMsgResponse);
        return new ScreenHandlerResult(newState, finalScreenResponsePayload);
    }
}
