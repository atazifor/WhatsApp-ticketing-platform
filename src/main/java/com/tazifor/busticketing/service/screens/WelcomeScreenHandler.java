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

@Component("WELCOME_SCREEN")
public class WelcomeScreenHandler implements ScreenHandler {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreenHandler.class);

    @Override
    public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {
        logger.info("WelcomeScreenHandler.handleDataExchange" + payload);
        Object selectedOption = payload.getData().get("selected_option");
        String option = "unsure";
        if (selectedOption instanceof Collection<?>) {
            Collection<?> options = (Collection<?>) selectedOption;
            option = options.iterator().next().toString();
        }
        state.setStep(option); // Track user's initial choice

        switch (option) {
            case "book_ticket":
                // proceed to choose destination screen
                state.setStep(STEP_CHOOSE_ORIGIN);
                return buildChooseOriginScreen();

            case "faq":
                // later implement FAQ
                return buildFaqScreen(payload.getFlow_token());

            case "support":
                // later implement Support
                return buildSupportScreen(payload.getFlow_token());

            default:
                throw new IllegalArgumentException("Unknown option: " + selectedOption);
        }
    }

    private FlowResponsePayload buildChooseOriginScreen() {
        List<Map<String, String>> cities = List.of(
            Map.of("id", "yaounde", "title", "Yaounde"),
            Map.of("id", "douala", "title", "Douala"),
            Map.of("id", "buea", "title", "Buea"),
            Map.of("id", "bamenda", "title", "Bamenda")
        );

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
