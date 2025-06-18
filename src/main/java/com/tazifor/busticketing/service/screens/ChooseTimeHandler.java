package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.dto.BookingState;
import com.tazifor.busticketing.service.MetadataLookupService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_SELECT_FILTERS;
import static com.tazifor.busticketing.util.BookingFormatter.extractList;
import static org.springframework.util.StringUtils.capitalize;


@Component("CHOOSE_TIME")
@RequiredArgsConstructor
public class ChooseTimeHandler implements ScreenHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChooseTimeHandler.class);

    private final MetadataLookupService lookupService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        // Extract selected time arrays from each bucket (may be null)
        List<String> morning = extractList(payload.getData().get("selected_morning_times"));
        List<String> afternoon = extractList(payload.getData().get("selected_afternoon_times"));
        List<String> evening = extractList(payload.getData().get("selected_evening_times"));

        // Combine all selected times into a single list
        List<String> allTimes = new ArrayList<>();
        allTimes.addAll(morning);
        allTimes.addAll(afternoon);
        allTimes.addAll(evening);

        BookingState newState = state.withSelectedTimes(allTimes)
            .withStep(STEP_SELECT_FILTERS);

        List<Map<String, Object>> classChips = lookupService.getAvailableTravelClasses().stream()
            .map(cls -> Map.<String, Object>of(
                "id", cls,
                "title", capitalize(cls),
                "enabled", true
            )).toList();

        List<Map<String, Object>> agencyChips = lookupService.getAvailableAgencies().stream()
            .map(agency -> Map.<String, Object>of(
                "id", agency,
                "title", agency,
                "enabled", true
            )).toList();

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_SELECT_FILTERS, Map.of(
            "class_options", classChips,
            "agency_options", agencyChips,
            "selected_classes", List.of(),
            "selected_agencies", List.of()
        ));

        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
