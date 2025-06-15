package com.tazifor.busticketing.model;

import java.util.List;

public class ScheduleDetails {
    private final AgencySchedule schedule;
    private final SeatingInfo seatingInfo;

    public ScheduleDetails(AgencySchedule schedule, SeatingInfo seatingInfo) {
        this.schedule = schedule;
        this.seatingInfo = seatingInfo;
    }

    public boolean isSoldOut() {
        return seatingInfo.availableInClass(schedule.travelClass()) == 0;
    }

    public boolean purchaseTicket(String seatNumber) {
        return seatingInfo.markSeatAsSold(schedule.travelClass(), seatNumber);
    }

    public List<Seat> availableSeats() {
        return seatingInfo.getAvailableSeats(schedule.travelClass());
    }

    public AgencySchedule schedule() {
        return schedule;
    }

    public SeatingInfo seatingInfo() {
        return seatingInfo;
    }
}

