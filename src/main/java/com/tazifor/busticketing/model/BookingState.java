package com.tazifor.busticketing.model;

import lombok.Data;

import java.util.Collection;

/**
 * Simple in‐memory state for the example bus‐ticket flow.
 */
@Data
public class BookingState {
    private String step;
    private Collection<String> selectedOption;
    private String origin;
    private String destination;
    private String date;
    private String time;
    private Collection<String> chosenSeats;
    private String fullName;
    private String email;
    private String phone;
    private String numTickets;
    private String moreDetails;
}
