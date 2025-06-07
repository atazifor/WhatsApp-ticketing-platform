package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FinalScreenResponsePayload;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_ORIGIN;

@Component("GENERIC_ERROR")
public class ErrorScreenHandler implements ScreenHandler {

    private static final Logger logger = LoggerFactory.getLogger(ErrorScreenHandler.class);

    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        logger.info("WelcomeScreenHandler.handleDataExchange" + payload);

        String errorMessage = payload.getData().getOrDefault("error_message", "An unknown error occurred.").toString();

        logger.warn("⚠️ WhatsApp Flow component error: {}", errorMessage);

        //It does not matter the name of the next screen. for now use GENERIC_ERROR
        return new NextScreenResponsePayload("GENERIC_ERROR", Map.of(
            "error_message", errorMessage
        ));
    }
}
