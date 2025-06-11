package com.tazifor.busticketing.service;

import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.repository.ScheduleRepository;
import com.tazifor.busticketing.util.schedule.ScheduleTimeGrouper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DepartureTimeSlotService {
    private final ScheduleRepository scheduleRepository;
    private volatile Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, String>>> cachedGroupedSlots;

    public Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, String>>> getGroupedTimeSlots() {
        if (cachedGroupedSlots == null) {
            synchronized (this) {
                if (cachedGroupedSlots == null) {
                    List<AgencySchedule> allSchedules = scheduleRepository.getAllSchedules(); // or a specialized method
                    cachedGroupedSlots = ScheduleTimeGrouper.groupByTimeSlot(allSchedules);
                }
            }
        }
        return cachedGroupedSlots;
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
        cachedGroupedSlots = null;
    }
}
