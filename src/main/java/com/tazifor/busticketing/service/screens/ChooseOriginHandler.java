package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_DESTINATION;
import static org.springframework.util.StringUtils.capitalize;

@Component("CHOOSE_ORIGIN")
public class ChooseOriginHandler implements ScreenHandler {

    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        String origin = payload.getData().get("origin").toString();
        state.setOrigin(origin);
        state.setStep(STEP_CHOOSE_DESTINATION);

        List<String> allCities = List.of("yaounde", "douala", "buea", "bamenda");

        List<Map<String, String>> destinations = allCities.stream()
            .filter(city -> !city.equals(origin))
            .map(city -> Map.of("id", city, "title", capitalize(city)))
            .toList();

        String introText = "You're traveling from *" + capitalize(origin) + "*. Great choice! \n" +
            "Now choose your destination";

        return new NextScreenResponsePayload(STEP_CHOOSE_DESTINATION, Map.of(
            "origin", origin,
            "origin_city_intro_text", introText,
            "destinations", destinations
        ));
    }

}
