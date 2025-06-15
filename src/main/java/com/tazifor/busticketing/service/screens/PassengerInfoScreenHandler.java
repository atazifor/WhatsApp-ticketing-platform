package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.model.Passenger;
import com.tazifor.busticketing.util.BookingFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.tazifor.busticketing.service.Screen.*;

@Component("PASSENGER_INFO")
public class PassengerInfoScreenHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(PassengerInfoScreenHandler.class);

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        Map<String, Object> data = payload.getData();
        String fullName = data.get("full_name").toString();
        String email = Optional.ofNullable(data.get("email"))
            .map(Object::toString)
            .orElse("");
        String phone = Optional.ofNullable(data.get("phone"))
            .map(Object::toString)
            .orElse("");

        // Deserialize or init list
        List<Passenger> passengerList = state.getPassengerList() != null
            ? new ArrayList<>(state.getPassengerList())
            : new ArrayList<>();
        passengerList.add(new Passenger(fullName, email, phone));

        String moreDetails= data.getOrDefault("more_details", "").toString();

        int totalTickets = Integer.parseInt(state.getNumTickets());
        boolean isLastPassenger = passengerList.size() >= totalTickets;

        // Update state
        BookingState newState = state
            .withMoreDetails(moreDetails)
            .withPassengerList(passengerList)
            .withStep(isLastPassenger ? STEP_SUMMARY : STEP_PASSENGER_INFORMATION);

        Map<String, Object> nextData = new LinkedHashMap<>();
        if(isLastPassenger) {
            // Build SUMMARY screen data
            String summary = BookingFormatter.buildSummaryText(newState);
            nextData.put("summary_text", summary);

            NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_SUMMARY, nextData);
            return new ScreenHandlerResult(newState, nextScreenResponsePayload);
        }else {
            nextData.put("current_passenger_index", passengerList.size() + 1+"");
            nextData.put("full_name", "");
            return new ScreenHandlerResult(newState,
                new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION, nextData));
        }

    }
}
