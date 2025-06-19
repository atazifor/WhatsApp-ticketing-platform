package com.nourri.busticketing.repository;

import com.nourri.busticketing.model.Schedule;
import com.nourri.busticketing.model.ScheduleClassPrice;
import com.nourri.busticketing.model.TravelClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleClassPriceRepository extends JpaRepository<ScheduleClassPrice, UUID> {
    Optional<ScheduleClassPrice> findByScheduleAndTravelClass(Schedule schedule, TravelClass travelClass);
    List<ScheduleClassPrice> findBySchedule(Schedule schedule);
}
