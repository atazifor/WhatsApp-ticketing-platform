package com.tazifor.busticketing.service;

import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.model.ScheduleDetails;
import com.tazifor.busticketing.repository.ScheduleDetailsLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService {
    private final ScheduleDetailsLoader loader;

    public List<AgencySchedule> findSchedules(String origin, String destination, String date,
                                              List<String> classes, List<String> agencies,
                                              List<String> departureTimes) {
        return loader.getAllSchedules().stream()
            .map(ScheduleDetails::schedule)
            .filter(s -> s.from().equalsIgnoreCase(origin))
            .filter(s -> s.to().equalsIgnoreCase(destination))
            .filter(s -> s.hasClassIn(classes))
            .filter(s -> s.isOperatedBy(agencies))
            .filter(s -> s.departsAt(departureTimes))
            .toList();
    }

    public Optional<ScheduleDetails> findScheduleDetails(String agency, String from, String to, String travelClass, String departureTime) {
        return loader.getAllSchedules().stream()
            .filter(sd -> agency.equalsIgnoreCase(sd.schedule().agency()))
            .filter(sd -> from.equalsIgnoreCase(sd.schedule().from()))
            .filter(sd -> to.equalsIgnoreCase(sd.schedule().to()))
            .filter(sd -> travelClass.equalsIgnoreCase(sd.schedule().travelClass()))
            .filter(sd -> departureTime.equals(sd.schedule().time()))
            .findFirst();
    }
}
