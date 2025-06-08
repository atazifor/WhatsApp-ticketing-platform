package com.tazifor.busticketing.model;

import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Simple in‐memory state for the example bus‐ticket flow.
 */

@RequiredArgsConstructor
@With
@Getter
public final class BookingState {
    private final String step;
    private final List<String> selectedOption;
    private final String origin;
    private final String destination;
    private final String date;
    private final List<String> selectedTimes;
    private final String time;
    private final List<String> selectedClasses;
    private final String travelClass;
    private final String agency;
    private final List<String> selectedAgencies;
    private final List<String> chosenSeats;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String numTickets;
    private final String moreDetails;

    public static BookingState empty() {
        return new BookingState(
            null, // step
            Collections.emptyList(), // selectedOption
            null, // origin
            null, // destination
            null, // date
            Collections.emptyList(), // selectedTimes
            null, // time
            Collections.emptyList(), // selectedClasses
            null, // travelClass
            null, // agency
            Collections.emptyList(), // selectedAgencies
            Collections.emptyList(), // chosenSeats
            null, // fullName
            null, // email
            null, // phone
            null, // numTickets
            null  // moreDetails
        );
    }
}
