package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_DATE;

@Component("CHOOSE_DESTINATION")
public class ChooseDestinationHandler implements ScreenHandler{
    private final static Logger logger = LoggerFactory.getLogger(ChooseDestinationHandler.class);

    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        String destination = payload.getData().get("destination").toString();
        state.setDestination(destination);
        state.setStep(STEP_CHOOSE_DATE); //next screen

        List<Map<String, String>> dates = List.of(
            Map.of("id", "2025-06-10", "title", "Tue Jun 10 2025"),
            Map.of("id", "2025-06-11", "title", "Wed Jun 11 2025"),
            Map.of("id", "2025-06-12", "title", "Thu Jun 12 2025")
        );
        return new NextScreenResponsePayload(STEP_CHOOSE_DATE, Map.of(
            "destination", destination,
            "dates", dates
        ));
    }
}
