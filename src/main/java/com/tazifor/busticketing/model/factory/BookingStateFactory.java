package com.tazifor.busticketing.model.factory;

import com.tazifor.busticketing.model.BookingState;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BookingStateFactory {

    /** Returns a completely empty BookingState */
    public static BookingState empty() {
        return BookingState.empty();
    }

    /** Starts a new booking flow from the given step */
    public static BookingState startNewAt(String step) {
        return BookingState.empty().withStep(step);
    }

    /**
     * Creates a BookingState from a generic Map<String, Object> payload.
     * Useful when rebuilding from WhatsApp data_exchange input.
     */
    public static BookingState fromPayload(Map<String, Object> data, String step) {
        return BookingState.empty()
            .withStep(step)
            .withSelectedOption(asStringList(data.get("selected_option")))
            .withOrigin(asString(data.get("origin")))
            .withDestination(asString(data.get("destination")))
            .withDate(asString(data.get("date")))
            .withSelectedTimes(asStringList(data.get("selected_times")))
            .withTime(asString(data.get("time")))
            .withSelectedClasses(asStringList(data.get("selected_classes")))
            .withTravelClass(asString(data.get("class")))
            .withAgency(asString(data.get("agency")))
            .withSelectedAgencies(asStringList(data.get("selected_agencies")))
            .withChosenSeats(asStringList(data.get("seat")))
            .withFullName(asString(data.get("full_name")))
            .withEmail(asString(data.get("email")))
            .withPhone(asString(data.get("phone")))
            .withNumTickets(asString(data.get("num_tickets")))
            .withMoreDetails(asString(data.get("more_details")));
    }

    private static String asString(Object obj) {
        return obj == null ? null : obj.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object obj) {
        if (obj instanceof List<?>) {
            return ((List<?>) obj).stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }
}
