package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepositoryCustom {
    Optional<Schedule> findScheduleDetails(String agency, String from, String to,
                                           String travelClass, String departureTime, String travelDate);

    List<Schedule> findScheduleByDate(LocalDate date);

    List<Schedule> findSchedules(String origin, String destination, String travelDate, List<String> classes, List<String> agencies, List<String> departureTimes);
}