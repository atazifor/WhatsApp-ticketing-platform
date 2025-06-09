package com.tazifor.busticketing.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Collections;
import java.util.List;

/**
 * Simple in‐memory state for the example bus‐ticket flow.
 */

@With
@Getter
@ToString
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

    @JsonCreator
    public BookingState(
        @JsonProperty("step") String step,
        @JsonProperty("selectedOption") List<String> selectedOption,
        @JsonProperty("origin") String origin,
        @JsonProperty("destination") String destination,
        @JsonProperty("date") String date,
        @JsonProperty("selectedTimes") List<String> selectedTimes,
        @JsonProperty("time") String time,
        @JsonProperty("selectedClasses") List<String> selectedClasses,
        @JsonProperty("travelClass") String travelClass,
        @JsonProperty("agency") String agency,
        @JsonProperty("selectedAgencies") List<String> selectedAgencies,
        @JsonProperty("chosenSeats") List<String> chosenSeats,
        @JsonProperty("fullName") String fullName,
        @JsonProperty("email") String email,
        @JsonProperty("phone") String phone,
        @JsonProperty("numTickets") String numTickets,
        @JsonProperty("moreDetails") String moreDetails
    ) {
        this.step = step;
        this.selectedOption = selectedOption;
        this.origin = origin;
        this.destination = destination;
        this.date = date;
        this.selectedTimes = selectedTimes;
        this.time = time;
        this.selectedClasses = selectedClasses;
        this.travelClass = travelClass;
        this.agency = agency;
        this.selectedAgencies = selectedAgencies;
        this.chosenSeats = chosenSeats;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.numTickets = numTickets;
        this.moreDetails = moreDetails;
    }



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
