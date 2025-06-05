package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.service.SeatService;
import com.tazifor.busticketing.util.BeanUtil;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.tazifor.busticketing.service.Screen.STEP_PASSENGER_INFORMATION;

@Component("CHOOSE_SEAT")
public class ChooseSeatHandler implements ScreenHandler {
    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        // 1) The user just tapped a chip (e.g. "B3")
        Object chosenSeats = payload.getData().get("seat");
        Collection<String> seats = (Collection<String>) chosenSeats;
        state.setChosenSeats(seats);

        // 2) Persist or mark the seat as taken if needed
        SeatService seatService = BeanUtil.getBean(SeatService.class);
        seats.forEach(seat -> {
            seatService.markSeatTaken(payload.getFlow_token(), seat);
        });


        // 3) Now advance to PASSENGER_INFO
        state.setStep(STEP_PASSENGER_INFORMATION);

        // 4) Build the PASSENGER_INFO payload (ask for name/email/phone/etc)
        //    This is exactly the same shape as before, e.g.:
        Map<String,Object> fields = new LinkedHashMap<>();
        fields.put("destination", state.getDestination());
        fields.put("date",        state.getDate());
        fields.put("time",        state.getTime());
        fields.put("seat",        chosenSeats);

        // The Flow builder (in your Companion JSON) expects something like:
        // { "type":"text_entry", "name":"full_name", "label":"Full Name" }, etc.
        // But if your PASSENGER_INFO is built via NextScreenResponsePayload, you might structure it as:
        //    new NextScreenResponsePayload("PASSENGER_INFO", fields);
        return new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION, fields);
    }

    private Collection<String> toSeatCollection(Object chosenSeats) {
        Collection<String> seats;
        if (chosenSeats == null) {
            seats = Collections.emptyList();
        } else if (chosenSeats instanceof Collection<?> c) {
            seats = c.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
        } else {
            seats = Collections.singletonList(chosenSeats.toString());
        }
        return seats;
    }
}
