package com.tazifor.busticketing;

import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.service.screens.SelectFiltersHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ScheduleRepository {
    private final List<AgencySchedule> schedules;

    public ScheduleRepository() {
        this.schedules = List.of(
            new AgencySchedule("United Express", "douala", "yaounde", "VIP", 10000, "08:00"),
            new AgencySchedule("United Express", "douala", "yaounde", "Regular", 8000, "11:00"),
            new AgencySchedule("Afrique con Plc", "yaounde", "buea", "VIP", 10000, "09:00"),
            new AgencySchedule("Afrique con Plc", "yaounde", "buea", "Regular", 8000, "13:00"),
            new AgencySchedule("Musango Bus Service", "yaounde", "buea", "VIP", 10000, "07:30"),
            new AgencySchedule("Musango Bus Service", "yaounde", "buea", "Regular", 6000, "14:00"),
            new AgencySchedule("Vatican Express", "yaounde", "bamenda", "VIP", 10000, "06:00"),
            new AgencySchedule("Vatican Express", "yaounde", "bamenda", "Regular", 7000, "10:00"),
            new AgencySchedule("Vatican Express", "bamenda", "buea", "Regular", 6500, "16:00"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "Master Class_", 25000, "06:30"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "Master Class", 20000, "08:30"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "VIP", 10000, "12:00"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "Regular", 8000, "15:00"),
            new AgencySchedule("Parklane Travels", "yaounde", "douala", "VIP", 8000, "09:00"),
            new AgencySchedule("Parklane Travels", "yaounde", "douala", "Regular", 6000, "11:30"),
            new AgencySchedule("Parklane Travels", "yaounde", "buea", "VIP", 10000, "13:00"),
            new AgencySchedule("Parklane Travels", "yaounde", "buea", "Regular", 8000, "16:30"),
            new AgencySchedule("NSO BOYZ EXPRESS", "yaounde", "bamenda", "VIP", 10000, "08:00"),
            new AgencySchedule("NSO BOYZ EXPRESS", "yaounde", "bamenda", "Regular", 7000, "13:00"),
            new AgencySchedule("NSO BOYZ EXPRESS", "bamenda", "buea", "Regular", 5500, "17:00"),
            new AgencySchedule("Touristique Express", "yaounde", "douala", "VIP", 10000, "09:45"),
            new AgencySchedule("Touristique Express", "yaounde", "douala", "Regular", 8000, "14:00"),
            new AgencySchedule("Finexs Voyages", "yaounde", "douala", "VIP", 8000, "07:00"),
            new AgencySchedule("Finexs Voyages", "yaounde", "douala", "Regular", 5000, "10:00"),
            new AgencySchedule("General Express Voyages Mvan", "yaounde", "douala", "VIP", 8000, "06:00"),
            new AgencySchedule("General Express Voyages Mvan", "yaounde", "douala", "Regular", 5000, "08:30")
        );
    }

    public List<AgencySchedule> getAllSchedules() {
        return schedules;
    }

    public List<String> getAvailableCities() {
        return schedules.stream()
            .flatMap(schedule ->
                Stream.of(schedule.from(), schedule.to()))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public List<String> getAvailableAgencies() {
        return schedules.stream()
            .map(AgencySchedule::agency)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    public List<String> getAvailableTravelClasses() {
        return schedules.stream()
            .map(AgencySchedule::travelClass)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /*@date filtering not yet implemented*/
    public List<AgencySchedule> findSchedules(String origin, String destination, String date, List<String> classes, List<String> agencies, List<String> departureTimes) {
//        return schedules.stream()
//            .filter(s -> s.from().equalsIgnoreCase(origin))
//            .filter(s -> s.to().equalsIgnoreCase(destination))
//            .filter(s -> classes.isEmpty() || classes.contains(s.travelClass()))
//            .filter(s -> agencies.isEmpty() || agencies.contains(s.agency()))
//            .filter(s -> departureTimes.isEmpty() || departureTimes.contains(s.time()))
//            .collect(Collectors.toList());
        return schedules.stream()
            .filter(s -> s.from().equalsIgnoreCase(origin))
            .filter(s -> s.to().equalsIgnoreCase(destination))
            .filter(s -> s.hasClassIn(classes))          // or true if classes is empty
            .filter(s -> s.isOperatedBy(agencies))       // or true if agencies is empty
            .filter(s -> s.departsAt(departureTimes))
            .collect(Collectors.toList());
    }
}
