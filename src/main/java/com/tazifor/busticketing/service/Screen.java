package com.tazifor.busticketing.service;

import com.tazifor.busticketing.model.BookingState;

import java.util.*;

public class Screen {

    public static final String STEP_CHOOSE_SEAT = "CHOOSE_SEAT";
    public static final String STEP_CHOOSE_DATE = "CHOOSE_DATE";
    public static final String STEP_CHOOSE_TIME = "CHOOSE_TIME";
    public static final String STEP_PASSENGER_INFORMATION = "PASSENGER_INFO";
    public static final String STEP_SUMMARY = "SUMMARY";

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

        return "*🗓 Appointment:* " + summaryData.get("appointment") + "\n" +
            "*📝 Details:* "     + summaryData.get("details")     + "\n\n" +
            "*📍 Destination:* " + summaryData.get("destination") + "\n" +
            "*📅 Date:* "        + summaryData.get("date")        + "\n" +
            "*⏰ Time:* "        + summaryData.get("time")        + "\n" +
            "*💺 Seat(s):* "         + seatDisplay        + "\n" +
            "*🎟 Tickets:* "     + summaryData.get("num_tickets") + "\n\n" +
            "_Any additional info:_ " + summaryData.get("more_details");
    }
}
