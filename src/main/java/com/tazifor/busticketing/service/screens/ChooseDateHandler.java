package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_TIME;

@Component("CHOOSE_DATE")
public class ChooseDateHandler implements ScreenHandler{
    private final static Logger logger = LoggerFactory.getLogger(ChooseDateHandler.class);

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        String date = payload.getData().get("date").toString();
        BookingState newState = state.withDate(date)
            .withStep(STEP_CHOOSE_TIME);

        // Grouped times by bucket
        List<Map<String, String>> morningSlots = List.of(
            Map.of("id", "04:00", "title", "04:00 AM"),
            Map.of("id", "08:00", "title", "08:00 AM"),
            Map.of("id", "10:00", "title", "10:00 AM")
        );

        List<Map<String, String>> afternoonSlots = List.of(
            Map.of("id", "12:00", "title", "12:00 PM"),
            Map.of("id", "15:00", "title", "03:00 PM")
        );

        List<Map<String, String>> eveningSlots = List.of(
            Map.of("id", "18:00", "title", "06:00 PM"),
            Map.of("id", "20:30", "title", "08:30 PM")
        );

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_TIME, Map.of(
            "origin", state.getOrigin(),
            "destination", state.getDestination(),
            "date", date,
            "morning_slots", morningSlots,
            "afternoon_slots", afternoonSlots,
            "evening_slots", eveningSlots,
            "selected_morning_times", List.of(), // Initially empty until user selects
            "selected_afternoon_times", List.of(),
            "selected_evening_times", List.of()
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
