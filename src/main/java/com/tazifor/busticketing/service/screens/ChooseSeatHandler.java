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
        BookingState newState = state.withChosenSeats(seats)
            .withStep(STEP_PASSENGER_INFORMATION);

        // 2) Persist or mark the seat as taken if needed
        SeatService seatService = BeanUtil.getBean(SeatService.class);
        seats.forEach(seat -> {
            seatService.markSeatTaken(payload.getFlow_token(), seat);
        });


        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION, Map.of());
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
