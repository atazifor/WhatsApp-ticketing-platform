package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.*;

@Component("PASSENGER_INFO")
public class PassengerInfoScreenHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(PassengerInfoScreenHandler.class);

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        logger.info("Passenger info screen handler called data {}", payload.getData());

        Map<String, Object> data = payload.getData();
        String fullName   = data.get("full_name").toString();
        String email      = data.get("email").toString();
        String phone      = data.get("phone").toString();
        String numTickets = data.get("num_tickets").toString();
        String moreDetails= data.getOrDefault("more_details", "").toString();

        // Update state
        BookingState newState = state
            .withFullName(fullName)
            .withEmail(email)
            .withPhone(phone)
            .withNumTickets(numTickets)
            .withMoreDetails(moreDetails)
            .withStep(STEP_SUMMARY);

        // Build SUMMARY screen data
        Map<String, Object> summaryData = new LinkedHashMap<>();
        String summary = buildSummaryText(newState);
        summaryData.put("summary_text", summary);

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_SUMMARY, summaryData);
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);

    }
}
