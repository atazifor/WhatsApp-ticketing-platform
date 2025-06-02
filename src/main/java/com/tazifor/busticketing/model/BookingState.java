package com.tazifor.busticketing.model;

import lombok.Data;

/**
 * Simple in‐memory state for the example bus‐ticket flow.
 */
@Data
public class BookingState {
    private String step;
    private String destination;
    private String date;
    private String time;
    private String fullName;
    private String email;
    private String phone;
    private String numTickets;
    private String moreDetails;
}
