package com.nourri.busticketing.util.schedule;

import com.nourri.busticketing.model.Schedule;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleTimeGrouper {

    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    public enum TimeSlotGroup {
        MORNING, AFTERNOON, EVENING
    }

    /**
     * Groups distinct departure times from Schedule entities into time slot buckets.
     *
     * @param schedules List of Schedule entities (from DB)
     * @return Map from TimeSlotGroup (MORNING, etc.) → List of Maps like {id: "14:30", title: "02:30 PM"}
     */
    public static Map<TimeSlotGroup, List<Map<String, Object>>> groupByTimeSlot(List<Schedule> schedules) {
        return schedules.stream()
            .map(Schedule::getDepartureTime)
            .distinct()
            .sorted()
            .map(time -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", time.toString());
                item.put("title", DISPLAY_TIME_FORMAT.format(time));
                return Map.entry(getTimeSlotGroup(time), item);
            })
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                LinkedHashMap::new,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            ));
    }

    private static TimeSlotGroup getTimeSlotGroup(LocalTime time) {
        if (time.isBefore(LocalTime.of(12, 0))) {
            return TimeSlotGroup.MORNING;
        } else if (time.isBefore(LocalTime.of(17, 0))) {
            return TimeSlotGroup.AFTERNOON;
        } else {
            return TimeSlotGroup.EVENING;
        }
    }
}
