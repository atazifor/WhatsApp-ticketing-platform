package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.ScheduleRepository;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_DESTINATION;
import static org.springframework.util.StringUtils.capitalize;

@Component("CHOOSE_ORIGIN")
@RequiredArgsConstructor
public class ChooseOriginHandler implements ScreenHandler {
    private final ScheduleRepository scheduleRepository;
    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        String origin = payload.getData().get("origin").toString();
        BookingState newState = state.withOrigin(origin)
            .withStep(STEP_CHOOSE_DESTINATION);

        List<String> allCities = scheduleRepository.getAvailableCities();

        List<Map<String, String>> destinations = allCities.stream()
            .filter(city -> !city.equals(origin))
            .map(city -> Map.of("id", city, "title", capitalize(city)))
            .toList();

        String introText = "You're traveling from **" + capitalize(origin) + "**. Great choice! \n" +
            "Now choose your destination";

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_DESTINATION, Map.of(
            "origin", origin,
            "origin_city_intro_text", introText,
            "destinations", destinations
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

}
