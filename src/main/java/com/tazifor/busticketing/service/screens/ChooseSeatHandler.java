package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.service.SeatService;
import com.tazifor.busticketing.util.BeanUtil;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.tazifor.busticketing.service.Screen.STEP_PASSENGER_INFORMATION;

@Component("CHOOSE_SEAT")
public class ChooseSeatHandler implements ScreenHandler {
    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        // 1) The user just tapped a chip (e.g. "B3")
        Object chosenSeats = payload.getData().get("seat");
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
        Map<String,Object> fields = new LinkedHashMap<>();
        fields.put("origin", state.getOrigin());
        fields.put("destination", state.getDestination());
        fields.put("date",        state.getDate());
        fields.put("time",        state.getTime());
        fields.put("class", state.getTravelClass());
        fields.put("agency", state.getAgency());
        fields.put("seat",        chosenSeats);

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION, fields);
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
