package com.tazifor.busticketing.service;

import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;

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
    public static final String STEP_PASSENGER_INFORMATION = "PASSENGER_INFO";
    public static final String STEP_SUMMARY = "SUMMARY";


    /** Builds the very first screen of an encrypted flow. */
    public static NextScreenResponsePayload buildInitialScreen(BookingState state) {

        state.setStep(STEP_WELCOME);

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

        return new NextScreenResponsePayload(
            STEP_WELCOME,
            Map.of("options", options)
        );
    }

    /** Handles “Back” by re‐showing the previous screen (stubbed as re‐initializing). */
    public static NextScreenResponsePayload showBackScreen(BookingState state) {
        return buildInitialScreen(state);
    }

    /**
     * Formats finalParams (excluding flow_token) into a user‐friendly string, e.g. "date=2025-05-31 destination=New York".
     */
    public static String formatSummaryDataForFinalImageMessageCaption(Map<String, Object> summaryData) {
        Object seatObj = summaryData.get("seat");
        String seatDisplay;

        if (seatObj == null) {
            seatDisplay = "Not selected";
        } else if (seatObj instanceof Collection<?>) {
            Collection<?> seats = (Collection<?>) seatObj;
            seatDisplay = seats.isEmpty() ? "Not selected" :
                seats.size() == 1 ? seats.iterator().next().toString() :
                    String.join(", ", seats.stream().map(Object::toString).toList());
        } else {
            seatDisplay = seatObj.toString();
        }

        return """
            🎟️ E-Ticket Confirmation
            --------------------------
            👤 Passenger:  %s
            📧 Email:      %s
            📱 Phone:      %s
            
            🚍 Agency:     %s
            💺 Class:      %s
            🛣️ From → To:  %s → %s
            📅 Date:       %s
            ⏰ Time:       %s
            🎫 Seat(s):    %s
            🔢 Tickets:    %s
            --------------------------
            📍 Please arrive 15 minutes early
            ✅ Safe travels with us!
            """.formatted(
            summaryData.get("full_name"),
            summaryData.get("email"),
            summaryData.get("phone"),
            summaryData.get("agency"),
            summaryData.get("class"),
            summaryData.get("origin"),
            summaryData.get("destination"),
            summaryData.get("date"),
            summaryData.get("time"),
            seatDisplay,
            summaryData.get("num_tickets")
        );

    }

    public static String formatAppointment(BookingState state) {
        return "%s on %s at %s".formatted(
            state.getDestination(),
            state.getDate(),
            state.getTime()
        );
    }

    public static String formatDetails(BookingState state) {
        return "Name: %s%nEmail: %s%nPhone: %s%n\"%s\"".formatted(
            state.getFullName(),
            state.getEmail(),
            state.getPhone(),
            state.getMoreDetails() == null ? "" : state.getMoreDetails()
        );
    }

    public static String buildSummaryText(Map<String, Object> summaryData) {
        Object seatObj = summaryData.get("seat");
        String seatDisplay;

        if (seatObj == null) {
            seatDisplay = "Not selected";
        } else if (seatObj instanceof Collection<?>) {
            Collection<?> seats = (Collection<?>) seatObj;
            seatDisplay = seats.isEmpty() ? "Not selected" :
                seats.size() == 1 ? seats.iterator().next().toString() :
                    String.join(", ", seats.stream().map(Object::toString).toList());
        } else {
            seatDisplay = seatObj.toString();
        }

        return "**Appointment:** " + summaryData.get("appointment") + "\n" +
            "**Details:** "     + summaryData.get("details")     + "\n\n" +
            "**Agency:** "      + summaryData.getOrDefault("agency", "N/A") + "\n" +
            "**Class:** "       + summaryData.getOrDefault("class", "N/A") + "\n" +
            "**Destination:** " + summaryData.get("destination") + "\n" +
            "**Date:** "        + summaryData.get("date")        + "\n" +
            "**Time:** "        + summaryData.get("time")        + "\n" +
            "**Seat(s):** "     + seatDisplay                    + "\n" +
            "**Tickets:** "     + summaryData.get("num_tickets") + "\n\n" +
            "_Any additional info:_ " + summaryData.get("more_details");
    }

}
