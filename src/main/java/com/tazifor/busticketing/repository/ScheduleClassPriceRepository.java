package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.Schedule;
import com.tazifor.busticketing.model.ScheduleClassPrice;
import com.tazifor.busticketing.model.TravelClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleClassPriceRepository extends JpaRepository<ScheduleClassPrice, UUID> {
    Optional<ScheduleClassPrice> findByScheduleAndTravelClass(Schedule schedule, TravelClass travelClass);
    List<ScheduleClassPrice> findBySchedule(Schedule schedule);
}
