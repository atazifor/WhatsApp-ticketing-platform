package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.*;

@Component("PASSENGER_INFO")
public class PassengerInfoScreenHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(SummaryScreenHandler.class);

    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        String fullName   = payload.getData().get("full_name").toString();
        String email      = payload.getData().get("email").toString();
        String phone      = payload.getData().get("phone").toString();
        String numTickets = payload.getData().get("num_tickets").toString();
        String moreDetails= payload.getData().getOrDefault("more_details", "").toString();

        // Update state
        state.setFullName(fullName);
        state.setEmail(email);
        state.setPhone(phone);
        state.setNumTickets(numTickets);
        state.setMoreDetails(moreDetails);
        state.setStep(STEP_SUMMARY);

        // Build SUMMARY screen data
        Map<String, Object> summaryData = new LinkedHashMap<>();
        summaryData.put("appointment",   formatAppointment(state));
        summaryData.put("details",       formatDetails(state));
        summaryData.put("destination",   state.getDestination());
        summaryData.put("date",          state.getDate());
        summaryData.put("time",          state.getTime());
        summaryData.put("seat",          state.getChosenSeats());
        summaryData.put("full_name",     fullName);
        summaryData.put("email",         email);
        summaryData.put("phone",         phone);
        summaryData.put("num_tickets",   numTickets);
        summaryData.put("more_details",  moreDetails);
        summaryData.put("summary_text",  buildSummaryText(summaryData));

        return new NextScreenResponsePayload(STEP_SUMMARY, summaryData);

    }
}
