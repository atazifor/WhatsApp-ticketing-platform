package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.ScheduleRepository;
import com.tazifor.busticketing.dto.*;
import com.tazifor.busticketing.model.BookingState;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_ORIGIN;
import static org.springframework.util.StringUtils.capitalize;

@Component("WELCOME_SCREEN")
@RequiredArgsConstructor
public class WelcomeScreenHandler implements ScreenHandler {

    private final ScheduleRepository scheduleRepository;

    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreenHandler.class);

    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        Object selectedOption = payload.getData().get("selected_option");
        String option = "unsure";
        if (selectedOption instanceof Collection<?>) {
            Collection<?> options = (Collection<?>) selectedOption;
            option = options.iterator().next().toString();
        }
        FlowResponsePayload flowResponsePayload;
        BookingState newState;
        switch (option) {
            case "book_ticket":
                // proceed to choose destination screen
                newState = state.withStep(STEP_CHOOSE_ORIGIN);
                flowResponsePayload = buildChooseOriginScreen();
                return new ScreenHandlerResult(newState, flowResponsePayload);

            case "faq":
                // later implement FAQ
                newState = state.withStep("faq_start");
                flowResponsePayload = buildFaqScreen(payload.getFlow_token());
                return new ScreenHandlerResult(newState, flowResponsePayload);

            case "support":
                // later implement Support
                newState = state.withStep("support");
                flowResponsePayload  = buildSupportScreen(payload.getFlow_token());
                return new ScreenHandlerResult(newState, flowResponsePayload);

            default:
                throw new IllegalArgumentException("Unknown option: " + selectedOption);
        }
    }

    private FlowResponsePayload buildChooseOriginScreen() {
        List<Map<String, String>> cities = scheduleRepository.getAvailableCities().stream()
            .map(city -> Map.of("id", city, "title", capitalize(city)))
            .toList();

        return new NextScreenResponsePayload("CHOOSE_ORIGIN", Map.of(
            "origins", cities
        ));
    }

    private FlowResponsePayload buildFaqScreen(String flowToken) {
        return new FinalScreenResponsePayload(
            new FinalScreenResponsePayload.ExtensionMessageResponse(
                Map.of("flow_token", flowToken,
                    "body", "🚌 *FAQs Coming Soon!* Please check back later.")
            )
        );
    }

    private FlowResponsePayload buildSupportScreen(String flowToken) {
        return new FinalScreenResponsePayload(
            new FinalScreenResponsePayload.ExtensionMessageResponse(
                Map.of( "flow_token", flowToken,
                    "body", "💬 *Support Coming Soon!* You’ll be able to chat with us directly shortly.")
            )
        );
    }
}
