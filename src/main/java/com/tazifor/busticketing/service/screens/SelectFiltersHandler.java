package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.service.ScheduleQueryService;
import com.tazifor.busticketing.service.ui.TripScheduleCardBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_DISPLAY_RESULTS;
import static com.tazifor.busticketing.util.BookingFormatter.extractList;
import static org.springframework.util.StringUtils.capitalize;


@Component("SELECT_FILTERS")
@RequiredArgsConstructor
public class SelectFiltersHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(SelectFiltersHandler.class);
    private final ScheduleQueryService queryService;
    private final TripScheduleCardBuilder tripScheduleCardBuilder;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        // Extract form data
        Map<String, Object> data = payload.getData();

        String origin = state.getOrigin();
        String destination = state.getDestination();
        String date = state.getDate();
        //String time = data.get("time").toString();
        List<String> selectedTimes = state.getSelectedTimes();
        List<String> selectedClasses = extractList(data.get("selected_classes"));
        List<String> selectedAgencies = extractList(data.get("selected_agencies"));

        logger.debug("Selected departure times: {}", selectedTimes);
        logger.debug("Selected classes: {}", selectedClasses);
        logger.debug("Selected agencies: {}", selectedAgencies);

        BookingState newState = state.withSelectedClasses(selectedClasses)
            .withSelectedAgencies(selectedAgencies);

        // Call filtering logic (mocked here)
        List<AgencySchedule> schedules = queryService.findSchedules(origin, destination, date, selectedClasses, selectedAgencies, selectedTimes);
        List<Map<String, Object>> matchingOptions = tripScheduleCardBuilder.build(schedules, date, newState);


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
                //"origin", origin,
                //"destination", destination,
                //"date", date,
                //"selected_times", selectedTimes,
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
            "trips", matchingOptions
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

}
