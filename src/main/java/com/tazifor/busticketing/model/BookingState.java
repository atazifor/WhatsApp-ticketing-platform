package com.tazifor.busticketing.model;

import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private List<String> selectedClasses;
    private String travelClass;
    private String agency;
    private List<String> selectedAgencies;
    private List<Map<String, Object>> matchingSchedules;
    private Collection<String> chosenSeats;
    private String fullName;
    private String email;
    private String phone;
    private String numTickets;
    private String moreDetails;
}
