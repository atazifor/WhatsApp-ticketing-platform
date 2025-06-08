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
    private final static Logger logger = LoggerFactory.getLogger(SummaryScreenHandler.class);

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        Map<String, Object> data = payload.getData();

        String fullName   = data.get("full_name").toString();
        String email      = data.get("email").toString();
        String phone      = data.get("phone").toString();
        String numTickets = data.get("num_tickets").toString();
        String moreDetails= data.getOrDefault("more_details", "").toString();

        // Update state
        BookingState newState = state.withFullName(fullName).withEmail(email)
            .withPhone(phone)
            .withNumTickets(numTickets)
            .withMoreDetails(moreDetails);

        // Defensive in case they weren’t yet set in state
        //if (data.containsKey("origin")) state.setOrigin(data.get("origin").toString());
        //if (data.containsKey("agency")) state.setAgency(data.get("agency").toString());
        //if (data.containsKey("class")) state.setTravelClass(data.get("class").toString());

        newState = state.withStep(STEP_SUMMARY);

        // Build SUMMARY screen data
        Map<String, Object> summaryData = new LinkedHashMap<>();
        summaryData.put("appointment",   formatAppointment(state));
        summaryData.put("details",       formatDetails(state));
        summaryData.put("origin",   state.getOrigin());
        summaryData.put("destination",   state.getDestination());
        summaryData.put("agency",   state.getAgency());
        summaryData.put("class",   state.getTravelClass());
        summaryData.put("date",          state.getDate());
        summaryData.put("time",          state.getTime());
        summaryData.put("seat",          state.getChosenSeats());
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
