package com.nourri.busticketing.service.screens;

import com.nourri.busticketing.dto.FlowDataExchangePayload;
import com.nourri.busticketing.dto.NextScreenResponsePayload;
import com.nourri.busticketing.dto.ScreenHandlerResult;
import com.nourri.busticketing.dto.BookingState;
import com.nourri.busticketing.service.SeatService;
import com.nourri.busticketing.util.BeanUtil;
import com.nourri.busticketing.whatsapp.session.SessionContextStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.nourri.busticketing.service.Screen.STEP_PASSENGER_INFORMATION;

@Component("CHOOSE_SEAT")
@RequiredArgsConstructor
public class ChooseSeatHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(ChooseSeatHandler.class);

    private final SessionContextStore sessionContextStore;

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

        String userWhatsAppPhoneNumber = sessionContextStore.getUserPhone(payload.getFlow_token()).orElse("");

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION,
            Map.of(
                "user_wa_number", userWhatsAppPhoneNumber,
                "current_passenger_index", Integer.parseInt(state.getNumTickets()) > 0 ? "1" : ""
            )
        );
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
