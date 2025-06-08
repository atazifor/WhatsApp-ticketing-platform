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
        // Check if the user agreed to terms
        boolean agreed = Boolean.parseBoolean(
            payload.getData().getOrDefault("agree_terms", "false").toString()
        );

        if (!agreed) {
            // Re‐show SUMMARY with an error
            Map<String, Object> errorData = new LinkedHashMap<>();
            errorData.put("appointment",    formatAppointment(state));
            errorData.put("details",        formatDetails(state));
            errorData.put("error_message",  "❗ You must agree to the terms to proceed.");
            return new ScreenHandlerResult(state.withStep(STEP_SUMMARY), new NextScreenResponsePayload(STEP_SUMMARY, errorData));
        }

        // Otherwise, finalize booking
        Map<String, Object> finalParams = new LinkedHashMap<>();
        finalParams.put("origin",  state.getOrigin());
        finalParams.put("destination",  state.getDestination());
        finalParams.put("date",         state.getDate());
        finalParams.put("time",         state.getTime());
        finalParams.put("class",         state.getTravelClass());
        finalParams.put("agency",         state.getAgency());
        finalParams.put("seat",          state.getChosenSeats());
        finalParams.put("full_name",    state.getFullName());
        finalParams.put("email",        state.getEmail());
        finalParams.put("phone",        state.getPhone());
        finalParams.put("num_tickets",  state.getNumTickets());
        finalParams.put("more_details", state.getMoreDetails());
        finalParams.put("flow_token",   payload.getFlow_token());

        var extMsgResponse = new FinalScreenResponsePayload.ExtensionMessageResponse(finalParams);
        FinalScreenResponsePayload finalScreenResponsePayload = new FinalScreenResponsePayload(extMsgResponse);
        return new ScreenHandlerResult(state.withStep("END"), finalScreenResponsePayload);
    }
}
