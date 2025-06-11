package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.*;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.*;

@Component("SUMMARY")
public class SummaryScreenHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(SummaryScreenHandler.class);

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
        String fullName = state.getFullName();
        String email = state.getEmail();
        String phone = state.getPhone();
        String moreDetails = state.getMoreDetails() != null ? state.getMoreDetails() : "";

        // Check if the user agreed to terms
        boolean agreed = Boolean.parseBoolean(
            payload.getData().getOrDefault("agree_terms", "false").toString()
        );

        if (!agreed) {
            // Re‐show SUMMARY with an error
            Map<String, Object> errorData = new LinkedHashMap<>();
            errorData.put("appointment",    formatAppointment(origin, destination, date, time));
            errorData.put("details",        formatDetails(fullName, email, phone, moreDetails));
            errorData.put("error_message",  "❗ You must agree to the terms to proceed.");
            return new ScreenHandlerResult(state.withStep(STEP_SUMMARY), new NextScreenResponsePayload(STEP_SUMMARY, errorData));
        }

        BookingState newState = state.withStep("END");

        // Otherwise, finalize booking
        Map<String, Object> finalParams = new LinkedHashMap<>();
        finalParams.put("origin", origin);
        finalParams.put("destination", destination);
        finalParams.put("date", date);
        finalParams.put("is_round_trip", isRoundTrip);
        finalParams.put("time", time);
        finalParams.put("class", travelClass);
        finalParams.put("agency", state.getAgency());
        finalParams.put("price", state.getPrice());
        finalParams.put("seat", state.getChosenSeats());
        finalParams.put("full_name", fullName);
        finalParams.put("email", email);
        finalParams.put("phone", phone);
        finalParams.put("num_tickets", state.getNumTickets());
        finalParams.put("more_details", moreDetails);
        finalParams.put("flow_token", payload.getFlow_token());

        var extMsgResponse = new FinalScreenResponsePayload.ExtensionMessageResponse(finalParams);
        FinalScreenResponsePayload finalScreenResponsePayload = new FinalScreenResponsePayload(extMsgResponse);
        return new ScreenHandlerResult(newState, finalScreenResponsePayload);
    }
}
