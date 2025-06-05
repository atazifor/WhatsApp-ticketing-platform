package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_TIME;

@Component("CHOOSE_DATE")
public class ChooseDateHandler implements ScreenHandler{
    private final static Logger logger = LoggerFactory.getLogger(ChooseDateHandler.class);


    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        String date = payload.getData().get("date").toString();
        state.setDate(date);
        state.setStep(STEP_CHOOSE_TIME);
        Object[] times = {
            Map.of("id", "08:00", "title", "08:00 AM"),
            Map.of("id", "10:00", "title", "10:00 AM"),
            Map.of("id", "12:00", "title", "12:00 PM")
        };
        return new NextScreenResponsePayload(STEP_CHOOSE_TIME, Map.of(
            "destination", state.getDestination(),
            "date", date,
            "times", times
        ));
    }
}
