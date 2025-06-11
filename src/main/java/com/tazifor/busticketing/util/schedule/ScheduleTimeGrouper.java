package com.tazifor.busticketing.util.schedule;

import com.tazifor.busticketing.model.AgencySchedule;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScheduleTimeGrouper {
    private static final DateTimeFormatter INPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    public enum TimeSlotGroup {
        MORNING, AFTERNOON, EVENING;
    }

    public static Map<TimeSlotGroup, List<Map<String, String>>> groupByTimeSlot(List<AgencySchedule> schedules) {
        return schedules.stream()
            .map(AgencySchedule::time)
            .distinct()
            .sorted()
            .map(time -> {
                LocalTime t = LocalTime.parse(time, INPUT_TIME_FORMAT);
                TimeSlotGroup group = getTimeSlotGroup(t);
                return Map.entry(group, Map.of("id", time, "title", DISPLAY_TIME_FORMAT.format(t)));
            })
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                LinkedHashMap::new,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            ));
    }

    private static TimeSlotGroup getTimeSlotGroup(LocalTime time) {
        if( time.isBefore(LocalTime.of(12, 0)) ) {
            return TimeSlotGroup.MORNING;
        } else if( time.isBefore(LocalTime.of(17, 0)) ) {
            return TimeSlotGroup.AFTERNOON;
        } else {
            return TimeSlotGroup.EVENING;
        }
    }
}
