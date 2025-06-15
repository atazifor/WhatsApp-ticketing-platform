package com.tazifor.busticketing.service;

import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.model.ScheduleDetails;
import com.tazifor.busticketing.repository.ScheduleDetailsLoader;
import com.tazifor.busticketing.util.schedule.CityPriorityUtil;
import com.tazifor.busticketing.util.schedule.ScheduleTimeGrouper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * StaticLookupService caches frequently accessed static or semi-static data
 * needed throughout the booking flow.
 *
 * <p>This includes:
 * <ul>
 *   <li>Grouped and sorted departure time slots (e.g., Morning, Afternoon)</li>
 *   <li>Sorted list of available cities with display-friendly titles and priorities</li>
 * </ul>
 *
 * <p>Data is precomputed once at startup and remains unchanged during runtime,
 * assuming the schedule data is mostly static. If future needs arise (e.g., refreshing),
 * this service can be extended to support periodic or manual cache invalidation.
 *
 * <p>Typical use cases:
 * <ul>
 *   <li>Providing dropdown values for cities</li>
 *   <li>Labeling schedules by time of day</li>
 * </ul>
 *
 * @see ScheduleDetailsLoader
 * @see CityPriorityUtil
 * @see ScheduleTimeGrouper
 */
@Component
@RequiredArgsConstructor
public class StaticLookupService {
    private final ScheduleDetailsLoader scheduleDetailsLoader;
    private volatile Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, String>>> timeSlots;
    private volatile List<String> cities;
    private volatile List<String> agencies;
    private volatile List<String> travelClasses;

    public Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, String>>> getGroupedTimeSlots() {
        if (timeSlots == null) {
            synchronized (this) {
                if (timeSlots == null) {
                    List<AgencySchedule> allSchedules = scheduleDetailsLoader.getAllSchedules().stream()
                        .map(ScheduleDetails::schedule)
                        .toList(); // or a specialized method
                    timeSlots = ScheduleTimeGrouper.groupByTimeSlot(allSchedules);
                }
            }
        }
        return timeSlots;
    }

    public List<String> getCities() {
        if (cities == null) {
            synchronized (this) {
                if (cities == null) {
                    List<String> availableCities = scheduleDetailsLoader.getAllSchedules().stream()
                        .flatMap(scheduleDetails -> Stream.of(scheduleDetails.schedule().from(), scheduleDetails.schedule().to() ) )
                        .distinct()
                        .toList();
                    cities = CityPriorityUtil.sortCitiesByPriority(availableCities);
                }
            }
        }
        return cities;
    }

    public List<String> getAvailableAgencies() {
        if (agencies == null) {
            synchronized (this) {
                if (agencies == null) {
                    agencies = scheduleDetailsLoader.getAllSchedules().stream()
                        .map(sd -> sd.schedule().agency())
                        .distinct()
                        .sorted()
                        .toList();
                }
            }
        }
        return agencies;
    }

    public List<String> getAvailableTravelClasses() {
        if (travelClasses == null) {
            synchronized (this) {
                if (travelClasses == null) {
                    travelClasses = scheduleDetailsLoader.getAllSchedules().stream()
                        .map(sd -> sd.schedule().travelClass())
                        .distinct()
                        .sorted()
                        .toList();
                }
            }
        }
        return travelClasses;
    }
    /**
     * <p>
     *Call departureTimeSlotService.invalidateCache() only when:
     * <ol>
     * <li>A new schedule is added</li>
     * <li>An existing schedule is removed</li>
     * <li>A major bulk update happens</li>
     * </ol>
     * </p>
     * This can be hooked into your admin interface or batch importer.
     */
    public void invalidateCache() {
        timeSlots = null;
        cities = null;
        agencies = null;
        travelClasses = null;
    }
}
