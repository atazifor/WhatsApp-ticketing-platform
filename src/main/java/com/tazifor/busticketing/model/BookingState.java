package com.tazifor.busticketing.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingState {
    private String step; // destination, date_selection, etc.
    private String destination;
    private String date;
    private String ticketNumber;
}
