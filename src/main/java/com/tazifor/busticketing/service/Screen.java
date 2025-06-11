package com.tazifor.busticketing.service;

import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
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

        int numTickets = Integer.parseInt(summaryData.get("num_tickets").toString());
        int oneWayPrice = Integer.parseInt(summaryData.get("price").toString());
        boolean isRoundTrip = Boolean.TRUE.equals(summaryData.get("is_round_trip"));

        String pricingBlock;
        if (isRoundTrip) {
            int fullPrice = oneWayPrice * 2 * numTickets;
            int discounted = (int) (fullPrice * 0.85);
            pricingBlock = """
            💰 Price:     ~~%d FCFA~~ %d FCFA (round trip 15%% off)
            🔁 Round trip selected!
            📞 Call 650000000 to confirm return
            """.formatted(fullPrice, discounted);
        } else {
            int totalPrice = oneWayPrice * numTickets;
            pricingBlock = "💰 Price:     %d FCFA".formatted(totalPrice);
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
        %s
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
            numTickets,
            pricingBlock
        );
    }


    public static String formatAppointment(String origin, String destination, String date, String time) {
        return "From %s -> %s on %s at %s".formatted(
            origin, destination, date, time
        );
    }

    public static String formatDetails(String name, String email, String phone, String moreDetails) {
        return "Name: %s%nEmail: %s%nPhone: %s%n\"%s\"".formatted(
            name,
            email,
            phone,
            moreDetails == null ? "" : moreDetails
        );
    }

    public static String buildSummaryText(BookingState state) {
        List<String> seats = state.getChosenSeats();
        String seatDisplay = seats.isEmpty() ? "Not selected" :
            seats.size() == 1 ? seats.iterator().next().toString() :
                String.join(", ", seats.stream().map(Object::toString).toList());

        String appointment = formatAppointment(state.getOrigin(), state.getDestination(), state.getDate(), state.getTime());
        String details = formatDetails(state.getFullName(), state.getEmail(), state.getPhone(), state.getMoreDetails());

        int pricePerTicket = Integer.parseInt(state.getPrice());
        int numTickets = Integer.parseInt(state.getNumTickets());
        int oneWayTotal = pricePerTicket * numTickets;

        boolean isRoundTrip = Boolean.TRUE.equals(state.isRoundTrip());

        String pricingSection;
        if (isRoundTrip) {
            int fullRoundTrip = oneWayTotal * 2;
            int discounted = (int) (fullRoundTrip * 0.85);

            pricingSection = "**Price:** ~~" + fullRoundTrip + " FCFA~~ " + discounted + " FCFA _(round trip 15% off)_";
        } else {
            pricingSection = "**Price:** " + oneWayTotal + " FCFA";
        }

        return "**Appointment:** " + appointment + "\n" +
            "**Details:** "     + details     + "\n\n" +
            "**Agency:** "      + state.getAgency() + "\n" +
            "**Class:** "       + state.getTravelClass() + "\n" +
            "**Destination:** " + state.getDestination() + "\n" +
            "**Date:** "        + state.getDate()        + "\n" +
            "**Time:** "        + state.getTime()        + "\n" +
            "**Seat(s):** "     + seatDisplay                    + "\n" +
            "**Tickets:** "     + numTickets + "\n" +
            pricingSection + "\n\n" +
            "_Any additional info:_ " + state.getMoreDetails();
    }


    public static List<String> extractList(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(raw.toString());
    }

}
