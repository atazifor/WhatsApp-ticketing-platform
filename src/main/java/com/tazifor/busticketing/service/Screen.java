package com.tazifor.busticketing.service;

import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.dto.BookingState;

import java.util.*;

public class Screen {

    public static final String STEP_WELCOME = "WELCOME_SCREEN";
    public static final String STEP_CHOOSE_ORIGIN = "CHOOSE_ORIGIN";
    public static final String STEP_CHOOSE_DESTINATION = "CHOOSE_DESTINATION";
    public static final String STEP_CHOOSE_SEAT = "CHOOSE_SEAT";
    public static final String STEP_CHOOSE_DATE = "CHOOSE_DATE";
    public static final String STEP_CHOOSE_TIME = "CHOOSE_TIME";
    public static final String STEP_SELECT_FILTERS = "SELECT_FILTERS";
    public static final String STEP_DISPLAY_RESULTS = "DISPLAY_RESULTS";
    public static final String STEP_NUMBER_OF_TICKETS = "NUMBER_OF_TICKETS";
    public static final String STEP_PASSENGER_INFORMATION = "PASSENGER_INFO";
    public static final String STEP_SUMMARY = "SUMMARY";


    /** Builds the very first screen of an encrypted flow. */
    public static ScreenHandlerResult buildInitialScreen(BookingState state) {
        BookingState newState = state.withStep(STEP_WELCOME);
        Object[] options = {
            Map.of("id", "book_ticket",
                "title", "🎟️ Book Ticket",
                "enabled", true),
            Map.of("id", "faq",
                "title", "❓ FAQs",
                "enabled", false),
            Map.of("id", "support",
                "title", "🎟️ View Past Bookings",
                "enabled", false)
        };

        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(
            STEP_WELCOME,
            Map.of("options", options)
        );
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }

    /** Handles “Back” by re‐showing the previous screen (stubbed as re‐initializing). */
    public static ScreenHandlerResult showBackScreen(BookingState state) {
        return buildInitialScreen(state);
    }

}
