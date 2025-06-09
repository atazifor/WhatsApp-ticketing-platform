package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.service.SeatService;
import com.tazifor.busticketing.util.BeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.tazifor.busticketing.service.Screen.STEP_PASSENGER_INFORMATION;

@Component("CHOOSE_SEAT")
public class ChooseSeatHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(ChooseSeatHandler.class);
    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        Map<String, Object> data = payload.getData();

        // 1) The user just tapped a chip (e.g. "B3")
        Object chosenSeats = data.get("seat");
        List<String> seats = (List<String>) chosenSeats;
        BookingState newState = state.withChosenSeats(seats);

        // 2) Persist or mark the seat as taken if needed
        SeatService seatService = BeanUtil.getBean(SeatService.class);
        seats.forEach(seat -> {
            seatService.markSeatTaken(payload.getFlow_token(), seat);
        });


        // 3) Now advance to PASSENGER_INFO
        newState = newState.withStep(STEP_PASSENGER_INFORMATION);


        // Build the final payload for passenger info
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("origin", data.get("origin").toString());
        fields.put("destination", data.get("destination").toString());
        fields.put("date", data.get("date").toString());
        fields.put("time", data.get("time").toString());
        fields.put("class", data.get("class").toString());
        fields.put("agency", data.get("agency").toString());
        fields.put("seat", chosenSeats);

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION, fields);
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
