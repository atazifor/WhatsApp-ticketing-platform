package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_SELECT_FILTERS;
import static org.springframework.util.StringUtils.capitalize;


@Component("CHOOSE_TIME")
public class ChooseTimeHandler implements ScreenHandler {
    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        String time = payload.getData().get("time").toString();
        state.setTime(time);
        state.setStep(STEP_SELECT_FILTERS);

        List<Map<String, Object>> classChips = getAvailableClasses().stream()
            .map(cls -> Map.<String, Object>of(
                "id", cls,
                "title", capitalize(cls),
                "enabled", true
            )).toList();

        List<Map<String, Object>> agencyChips = getAvailableAgencies().stream()
            .map(agency -> Map.<String, Object>of(
                "id", agency,
                "title", agency,
                "enabled", true
            )).toList();

        return new NextScreenResponsePayload(STEP_SELECT_FILTERS, Map.of(
            "origin", state.getOrigin(),
            "destination", state.getDestination(),
            "date", state.getDate(),
            "time", time,
            "class_options", classChips,
            "agency_options", agencyChips,
            "selected_classes", new ArrayList<>(),
            "selected_agencies", new ArrayList<>()
        ));
    }

    private List<String> getAvailableClasses() {
        return List.of("VIP", "Regular", "Master Class");
    }

    private List<String> getAvailableAgencies() {
        return List.of("United Express", "Finexs Voyages", "Musango", "Touristique Express");
    }

}
