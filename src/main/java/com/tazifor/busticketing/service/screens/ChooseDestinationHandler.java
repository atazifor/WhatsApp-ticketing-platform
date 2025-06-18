package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.dto.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_DATE;
import static org.springframework.util.StringUtils.capitalize;

@Component("CHOOSE_DESTINATION")
public class ChooseDestinationHandler implements ScreenHandler {
    private final static Logger logger = LoggerFactory.getLogger(ChooseDestinationHandler.class);

    public static final int BOOKING_WINDOW_DAYS = 15;

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload, BookingState state) {
        String origin = state.getOrigin();
        String destination = payload.getData().get("destination").toString();


        BookingState newState = state.withDestination(destination)
            .withStep(STEP_CHOOSE_DATE);


        String introText = "You're traveling from **" + capitalize(origin) + "** to **" + capitalize(destination) + "**. \n" +
            "When would you like to travel?";

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(BOOKING_WINDOW_DAYS);

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_DATE, Map.of(
            "today", today.toString(),
            "max_date", maxDate.toString(),
            "date_intro", introText
        ));
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
