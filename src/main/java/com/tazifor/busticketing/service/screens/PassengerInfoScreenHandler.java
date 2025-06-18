package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.dto.BookingState;
import com.tazifor.busticketing.dto.Passenger;
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
        String fullName = getValue(data, "full_name");
        String email = getValue(data, "email");
        String phone = getValue(data, "phone");
        String moreDetails = getValue(data, "more_details");

        // Deserialize or init list
        List<Passenger> passengerList = state.getPassengerList() != null
            ? new ArrayList<>(state.getPassengerList())
            : new ArrayList<>();
        passengerList.add(new Passenger(fullName, email, phone));


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

    private String getValue(Map<String, Object> data, String key) {
        return Optional.ofNullable(data.get(key)).map(Object::toString).orElse("");
    }
}
