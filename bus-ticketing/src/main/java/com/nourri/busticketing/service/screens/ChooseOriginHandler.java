package com.nourri.busticketing.service.screens;

import com.nourri.busticketing.dto.FlowDataExchangePayload;
import com.nourri.busticketing.dto.NextScreenResponsePayload;
import com.nourri.busticketing.dto.ScreenHandlerResult;
import com.nourri.busticketing.dto.BookingState;
import com.nourri.busticketing.service.MetadataLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.nourri.busticketing.service.Screen.STEP_CHOOSE_DESTINATION;
import static org.springframework.util.StringUtils.capitalize;

@Component("CHOOSE_ORIGIN")
@RequiredArgsConstructor
public class ChooseOriginHandler implements ScreenHandler {
    private final MetadataLookupService metadataLookupService;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {

        String origin = payload.getData().get("origin").toString();

        BookingState newState = state.withOrigin(origin)
            .withStep(STEP_CHOOSE_DESTINATION);

        List<String> allCities = metadataLookupService.getCities();

        List<Map<String, String>> destinations = allCities.stream()
            .filter(city -> !city.equals(origin))
            .map(city -> Map.of("id", city, "title", capitalize(city)))
            .toList();

        String introText = "You're traveling from **" + capitalize(origin) + "**. Great choice! \n" +
            "Now choose your destination";

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_DESTINATION, Map.of(
            //"origin", input.origin(),
            "origin_city_intro_text", introText,
            "destinations", destinations
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

}
