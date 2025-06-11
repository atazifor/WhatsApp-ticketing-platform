package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.service.DepartureTimeSlotService;
import com.tazifor.busticketing.util.schedule.ScheduleTimeGrouper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_TIME;

@Component("CHOOSE_DATE")
@RequiredArgsConstructor
public class ChooseDateHandler implements ScreenHandler{
    private final static Logger logger = LoggerFactory.getLogger(ChooseDateHandler.class);
    private final DepartureTimeSlotService departureTimeSlotService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        String date = payload.getData().get("date").toString();
        boolean isRoundTrip = payload.getData().getOrDefault("is_round_trip", false).toString().equals("true");

        BookingState newState = state.withDate(date)
            .withRoundTrip(isRoundTrip)
            .withStep(STEP_CHOOSE_TIME);;

        // Grouped times by bucket
        Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, String>>> groupedTimeSlots = departureTimeSlotService.getGroupedTimeSlots();

        List<Map<String, String>> morningSlots = groupedTimeSlots.get(ScheduleTimeGrouper.TimeSlotGroup.MORNING);

        List<Map<String, String>> afternoonSlots = groupedTimeSlots.get(ScheduleTimeGrouper.TimeSlotGroup.AFTERNOON);

        List<Map<String, String>> eveningSlots = groupedTimeSlots.get(ScheduleTimeGrouper.TimeSlotGroup.EVENING);

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_TIME, Map.of(
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
