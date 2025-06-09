package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
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
        String origin = data.get("origin").toString();
        String destination = data.get("destination").toString();
        String date = data.get("date").toString();
        String time = data.get("time").toString();
        String fullName   = data.get("full_name").toString();
        String email      = data.get("email").toString();
        String phone      = data.get("phone").toString();
        String numTickets = data.get("num_tickets").toString();
        String moreDetails= data.getOrDefault("more_details", "").toString();

        // Update state
        BookingState newState = state.withFullName(fullName).withEmail(email)
            .withPhone(phone)
            .withNumTickets(numTickets)
            .withMoreDetails(moreDetails)
            .withStep(STEP_SUMMARY);

        // Build SUMMARY screen data
        Map<String, Object> summaryData = new LinkedHashMap<>();
        summaryData.put("appointment",   formatAppointment(origin, destination, date, time));
        summaryData.put("details",       formatDetails(fullName, email, phone, moreDetails));
        summaryData.put("origin",   data.get("origin").toString());
        summaryData.put("destination",   data.get("destination").toString());
        summaryData.put("agency",   data.get("agency").toString());
        summaryData.put("class",   data.get("class").toString());
        summaryData.put("date",          data.get("date").toString());
        summaryData.put("time",          data.get("time").toString());
        summaryData.put("seat",          (List<String>)data.get("seat"));
        summaryData.put("full_name",     fullName);
        summaryData.put("email",         email);
        summaryData.put("phone",         phone);
        summaryData.put("num_tickets",   numTickets);
        summaryData.put("more_details",  moreDetails);
        summaryData.put("summary_text",  buildSummaryText(summaryData));

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_SUMMARY, summaryData);
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);

    }
}
