package com.tazifor.busticketing.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Ticket {
    private String ticketNumber;
    private String scheduleId;
    private String agency;
    private String agencyPhone;
    private String origin;
    private String destination;
    private String date;
    private String time;
    private String travelClass;

    private String seat; // optional
    private int price;

    private String passengerName;
    private String passengerPhone; // optional
    private String passengerEmail; // optional

    private boolean roundTrip;
    private LocalDateTime issuedAt;
}

