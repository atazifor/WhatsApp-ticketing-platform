package com.nourri.busticketing.service;

import com.nourri.busticketing.model.Agency;
import com.nourri.busticketing.model.Schedule;
import com.nourri.busticketing.model.TravelClass;
import com.nourri.busticketing.repository.AgencyRepository;
import com.nourri.busticketing.repository.ScheduleRepository;
import com.nourri.busticketing.repository.TravelClassRepository;
import com.nourri.busticketing.util.schedule.CityPriorityUtil;
import com.nourri.busticketing.util.schedule.ScheduleTimeGrouper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MetadataLookupService {

    private final ScheduleRepository scheduleRepository;
    private final AgencyRepository agencyRepository;
    private final TravelClassRepository travelClassRepository;

    @Cacheable("timeSlots")
    public Map<ScheduleTimeGrouper.TimeSlotGroup, List<Map<String, Object>>> getGroupedTimeSlots() {
        List<Schedule> allSchedules = scheduleRepository.findAll();
        return ScheduleTimeGrouper.groupByTimeSlot(allSchedules);
    }

    @Cacheable("cities")
    public List<String> getCities() {
        List<String> cityNames = scheduleRepository.findAll().stream()
            .flatMap(s -> Stream.of(
                s.getFromLocation().getName(),
                s.getToLocation().getName()))
            .distinct()
            .toList();
        return CityPriorityUtil.sortCitiesByPriority(cityNames);
    }

    @Cacheable("availableAgencies")
    public List<String> getAvailableAgencies() {
        return agencyRepository.findAll().stream()
            .map(Agency::getName)
            .distinct()
            .sorted()
            .toList();
    }

    @Cacheable("availableTravelClasses")
    public List<String> getAvailableTravelClasses() {
        return travelClassRepository.findAll().stream()
            .map(TravelClass::getName)
            .distinct()
            .sorted()
            .toList();
    }

    /**
     * Invalidate all lookup caches after bulk imports or admin changes.
     */
    @CacheEvict(cacheNames = {
        "timeSlots", "cities", "availableAgencies", "availableTravelClasses"
    }, allEntries = true)
    public void invalidateCache() {
        // No-op: Spring handles invalidation
    }
}