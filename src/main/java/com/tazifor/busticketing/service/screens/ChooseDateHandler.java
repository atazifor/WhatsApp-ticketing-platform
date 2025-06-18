package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.dto.BookingState;
import com.tazifor.busticketing.service.MetadataLookupService;
import com.tazifor.busticketing.util.schedule.ScheduleTimeGrouper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_TIME;
import static com.tazifor.busticketing.util.schedule.ScheduleTimeGrouper.TimeSlotGroup.*;

@Component("CHOOSE_DATE")
@RequiredArgsConstructor
public class ChooseDateHandler implements ScreenHandler{
    private final static Logger logger = LoggerFactory.getLogger(ChooseDateHandler.class);
    private final MetadataLookupService lookupService;


    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        String date = payload.getData().get("date").toString();
        boolean isRoundTrip = payload.getData().getOrDefault("is_round_trip", false).toString().equals("true");

        BookingState newState = state.withDate(date)
            .withRoundTrip(isRoundTrip)
            .withStep(STEP_CHOOSE_TIME);

        // Grouped times by bucket
        Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, Object>>> groupedTimeSlots = lookupService.getGroupedTimeSlots();

        List<Map<String, Object>> morningSlots = getSafeTimeSlotList(groupedTimeSlots, MORNING);
        List<Map<String, Object>> afternoonSlots = getSafeTimeSlotList(groupedTimeSlots, AFTERNOON);
        List<Map<String, Object>> eveningSlots = getSafeTimeSlotList(groupedTimeSlots, EVENING);

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_TIME, Map.of(
            "morning_slots", morningSlots,
            "afternoon_slots", afternoonSlots,
            "evening_slots", eveningSlots,
            "selected_morning_times", List.of(),
            "selected_afternoon_times", List.of(),
            "selected_evening_times", List.of()
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

    private List<Map<String, Object>> getSafeTimeSlotList(
        Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, Object>>> groupedTimeSlots,
        ScheduleTimeGrouper.TimeSlotGroup group
    ) {
        List<Map<String, Object>> slots = groupedTimeSlots.get(group);
        if (slots == null || slots.isEmpty()) {
            return List.of(
                Map.of(
                    "id", "placeholder_" + group.name().toLowerCase(),
                    "title", "No options available",
                    "enabled", false
                )
            );
        }
        return slots;
    }
}
