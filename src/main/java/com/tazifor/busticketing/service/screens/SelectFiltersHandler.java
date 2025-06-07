package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_DISPLAY_RESULTS;


@Component("SELECT_FILTERS")
public class SelectFiltersHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(SelectFiltersHandler.class);

    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        // Extract form data
        String origin = payload.getData().get("origin").toString();
        String destination = payload.getData().get("destination").toString();
        String date = payload.getData().get("date").toString();
        String time = payload.getData().get("time").toString();

        List<String> selectedClasses = getListFrom(payload.getData().get("selected_classes"));
        List<String> selectedAgencies = getListFrom(payload.getData().get("selected_agencies"));

        logger.debug("Selected classes: {}", selectedClasses);
        logger.debug("Selected agencies: {}", selectedAgencies);
        state.setOrigin(origin);
        state.setDestination(destination);
        state.setDate(date);
        state.setTime(time);
        state.setSelectedClasses(selectedClasses);
        state.setSelectedAgencies(selectedAgencies);

        // Call filtering logic (mocked here)
        List<Map<String, Object>> matchingOptions = findMatchingSchedules(origin, destination, date, time, selectedClasses, selectedAgencies);

        logger.info("Matching options: {}", matchingOptions);
        if (matchingOptions.isEmpty()) {
            String summary = "No trips found from **" + capitalize(origin) +
                "** to **" + capitalize(destination) +
                "** on " + date + " at " + time + ".";

            if (!selectedClasses.isEmpty() || !selectedAgencies.isEmpty()) {
                List<String> filters = new ArrayList<>();
                if (!selectedClasses.isEmpty()) {
                    filters.add("Classes - " + String.join(", ", selectedClasses));
                }
                if (!selectedAgencies.isEmpty()) {
                    filters.add("Agencies - " + String.join(", ", selectedAgencies));
                }
                summary += "\nFilters: " + String.join("; ", filters);
            }

            return new NextScreenResponsePayload("NO_DISPLAY_RESULTS", Map.of(
                "origin", origin,
                "destination", destination,
                "date", date,
                "time", time,
                "selected_classes", selectedClasses,
                "selected_agencies", selectedAgencies,
                "summary_text", summary
            ));
        }

        // Store matchingOptions in state if needed for further selection
        state.setMatchingSchedules(matchingOptions);
        state.setStep(STEP_DISPLAY_RESULTS);

        // Move to seat selection, or intermediate screen showing results
        return new NextScreenResponsePayload(STEP_DISPLAY_RESULTS, Map.of(
            "origin", origin,
            "destination", destination,
            "date", date,
            "time", time,
            "trips", matchingOptions
        ));
    }

    private List<String> getListFrom(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(raw.toString());
    }

    private List<Map<String, Object>> findMatchingSchedules(
        String origin,
        String destination,
        String date,
        String time,
        List<String> classes,
        List<String> agencies
    ) {
        // Replace with DB/cache call
        List<AgencySchedule> allSchedules = getSchedules();

        return allSchedules.stream()
            .filter(s -> s.from().equalsIgnoreCase(origin))
            .filter(s -> s.to().equalsIgnoreCase(destination))
            .filter(s -> s.hasClassIn(classes))          // or true if classes is empty
            .filter(s -> s.isOperatedBy(agencies))       // or true if agencies is empty
            .filter(s -> s.operatesAt(time))             // optional for now
            .map(schedule -> {
                String title = truncate(schedule.agency() + " - " + schedule.travelClass(), 30);
                String metadata = truncate(
                    capitalize(schedule.from()) + " → " + capitalize(schedule.to()) +
                        " | " + schedule.time() +
                        " | " + formatPrice(schedule.price()),
                    80
                );
                return Map.of(
                    "id", schedule.agency() + "_" + schedule.travelClass() + "_" + schedule.time(),
                    "main-content", Map.of(
                        "title", title,
                        "metadata", metadata
                    ),
                    "on-click-action", Map.of(
                        "name", "data_exchange",
                        "payload", Map.of(
                            "screen", "CHOOSE_SEAT",
                            "agency", schedule.agency(),
                            "origin", schedule.from(),
                            "destination", schedule.to(),
                            "class", schedule.travelClass(),
                            "date", date,
                            "time", schedule.time(),
                            "price", schedule.price()
                        )
                    )
                );
            })
            .toList();
    }

    private String formatPrice(int price) {
        return String.format("%,dF", price);
    }

    private String truncate(String str, int limit) {
        return (str.length() <= limit) ? str : str.substring(0, limit - 1) + "…";
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private List<AgencySchedule> getSchedules() {
        return List.of(
            // United Express
            new AgencySchedule("United Express", "douala", "yaounde", "VIP", 10000, "08:00"),
            new AgencySchedule("United Express", "douala", "yaounde", "Regular", 8000, "11:00"),

            // Afrique con Plc
            new AgencySchedule("Afrique con Plc", "yaounde", "buea", "VIP", 10000, "09:00"),
            new AgencySchedule("Afrique con Plc", "yaounde", "buea", "Regular", 8000, "13:00"),

            // Musango
            new AgencySchedule("Musango Bus Service", "yaounde", "buea", "VIP", 10000, "07:30"),
            new AgencySchedule("Musango Bus Service", "yaounde", "buea", "Regular", 6000, "14:00"),

            // Vatican Express
            new AgencySchedule("Vatican Express", "yaounde", "bamenda", "VIP", 10000, "06:00"),
            new AgencySchedule("Vatican Express", "yaounde", "bamenda", "Regular", 7000, "10:00"),
            new AgencySchedule("Vatican Express", "bamenda", "buea", "Regular", 6500, "16:00"),

            // Men Travel
            new AgencySchedule("Men Travel", "yaounde", "douala", "Master Class_", 25000, "06:30"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "Master Class", 20000, "08:30"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "VIP", 10000, "12:00"),
            new AgencySchedule("Men Travel", "yaounde", "douala", "Regular", 8000, "15:00"),

            // Parklane
            new AgencySchedule("Parklane Travels", "yaounde", "douala", "VIP", 8000, "09:00"),
            new AgencySchedule("Parklane Travels", "yaounde", "douala", "Regular", 6000, "11:30"),
            new AgencySchedule("Parklane Travels", "yaounde", "buea", "VIP", 10000, "13:00"),
            new AgencySchedule("Parklane Travels", "yaounde", "buea", "Regular", 8000, "16:30"),

            // NSO BOYZ
            new AgencySchedule("NSO BOYZ EXPRESS", "yaounde", "bamenda", "VIP", 10000, "08:00"),
            new AgencySchedule("NSO BOYZ EXPRESS", "yaounde", "bamenda", "Regular", 7000, "13:00"),
            new AgencySchedule("NSO BOYZ EXPRESS", "bamenda", "buea", "Regular", 5500, "17:00"),

            // Touristique
            new AgencySchedule("Touristique Express", "yaounde", "douala", "VIP", 10000, "09:45"),
            new AgencySchedule("Touristique Express", "yaounde", "douala", "Regular", 8000, "14:00"),

            // Finexs
            new AgencySchedule("Finexs Voyages", "yaounde", "douala", "VIP", 8000, "07:00"),
            new AgencySchedule("Finexs Voyages", "yaounde", "douala", "Regular", 5000, "10:00"),

            // General Express
            new AgencySchedule("General Express Voyages Mvan", "yaounde", "douala", "VIP", 8000, "06:00"),
            new AgencySchedule("General Express Voyages Mvan", "yaounde", "douala", "Regular", 5000, "08:30")
        );
    }

    private record AgencySchedule(String agency,
                                  String from,
                                  String to,
                                  String travelClass,
                                  int price,
                                  String time
    ) {
        public boolean hasClassIn(List<String> selectedClasses) {
            return selectedClasses.isEmpty() || selectedClasses.contains(travelClass);
        }

        public boolean isOperatedBy(List<String> selectedAgencies) {
            return selectedAgencies.isEmpty() || selectedAgencies.contains(agency);
        }

        public boolean operatesAt(String selectedTime) {
            return selectedTime == null || selectedTime.equals(time); // simple match for now
        }
    }
}
