package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.ScheduleRepository;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.model.BookingState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_DISPLAY_RESULTS;
import static com.tazifor.busticketing.service.Screen.extractList;


@Component("SELECT_FILTERS")
@RequiredArgsConstructor
public class SelectFiltersHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(SelectFiltersHandler.class);

    private final ScheduleRepository scheduleRepository;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        // Extract form data
        Map<String, Object> data = payload.getData();

        String origin = data.get("origin").toString();
        String destination = data.get("destination").toString();
        String date = data.get("date").toString();
        //String time = data.get("time").toString();
        List<String> selectedTimes = extractList(data.get("selected_times"));
        List<String> selectedClasses = extractList(data.get("selected_classes"));
        List<String> selectedAgencies = extractList(data.get("selected_agencies"));

        logger.debug("Selected departure times: {}", selectedTimes);
        logger.debug("Selected classes: {}", selectedClasses);
        logger.debug("Selected agencies: {}", selectedAgencies);

        BookingState newState = state.withSelectedClasses(selectedClasses)
            .withSelectedAgencies(selectedAgencies);

        // Call filtering logic (mocked here)
        List<AgencySchedule> schedules = scheduleRepository.findSchedules(origin, destination, date, selectedClasses, selectedAgencies, selectedTimes);
        List<Map<String, Object>> matchingOptions = buildUI(schedules, date);


        logger.info("Matching options: {}", matchingOptions);
        if (matchingOptions.isEmpty()) {
            String summary = "No trips found from **" + capitalize(origin) +
                "** to **" + capitalize(destination) +
                "** on " + date + ".";

            if (!selectedClasses.isEmpty()
                || !selectedAgencies.isEmpty()
                || !selectedTimes.isEmpty()
            ) {
                List<String> filters = new ArrayList<>();
                if (!selectedTimes.isEmpty()) {
                    filters.add("Departure Times - " + String.join(", ", selectedTimes));
                }
                if (!selectedClasses.isEmpty()) {
                    filters.add("Classes - " + String.join(", ", selectedClasses));
                }
                if (!selectedAgencies.isEmpty()) {
                    filters.add("Agencies - " + String.join(", ", selectedAgencies));
                }
                summary += "\nFilters: " + String.join("; ", filters);
            }
            newState = newState.withStep("NO_DISPLAY_RESULTS");
            NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload("NO_DISPLAY_RESULTS", Map.of(
                "origin", origin,
                "destination", destination,
                "date", date,
                "selected_times", selectedTimes,
                "selected_classes", selectedClasses,
                "selected_agencies", selectedAgencies,
                "summary_text", summary
            ));
            return new ScreenHandlerResult(newState, nextScreenResponsePayload);
        }

        // Store matchingOptions in state if needed for further selection
        newState = state.withStep(STEP_DISPLAY_RESULTS);

        // Move to seat selection, or intermediate screen showing results
        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_DISPLAY_RESULTS, Map.of(
            "origin", origin,
            "destination", destination,
            "date", date,
            "trips", matchingOptions
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

    private List<Map<String, Object>> buildUI(List<AgencySchedule> allSchedules, String date) {
        return allSchedules.stream()
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
}
