package com.nourri.busticketing.service;

import com.nourri.busticketing.exception.EntityNotFoundException;
import com.nourri.busticketing.model.Booking;
import com.nourri.busticketing.model.Schedule;
import com.nourri.busticketing.model.Seat;
import com.nourri.busticketing.model.Ticket;
import com.nourri.busticketing.repository.BookingRepository;
import com.nourri.busticketing.repository.ScheduleRepository;
import com.nourri.busticketing.repository.SeatRepository;
import com.nourri.busticketing.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles booking creation, ticket generation, and seat assignment.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;

    public Booking createBooking(String customerName, String phone, String email, String moreDetails,
                                 UUID scheduleId, List<String> seatNumbers, List<String> passengerNames) {

        // Validate inputs
        if (passengerNames == null || passengerNames.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }

        boolean isAssignedSeating = (seatNumbers != null && !seatNumbers.isEmpty());

        if (isAssignedSeating && seatNumbers.size() != passengerNames.size()) {
            throw new IllegalArgumentException("Number of seats must match number of passengers");
        }

        // Load schedule with all required relationships
        Schedule schedule = scheduleRepository.findByIdWithLock(scheduleId)
            .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));

        // Create and save booking first
        Booking booking = new Booking();
        booking.setCustomerName(customerName);
        booking.setPhone(phone);
        booking.setEmail(email);
        booking.setMoreDetails(moreDetails);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        // Fetch all seats for this schedule in one query
        List<Seat> scheduleSeats = seatRepository.findBySchedule(schedule);

        List<Ticket> tickets = new ArrayList<>();

        for (int i = 0; i < passengerNames.size(); i++) {
            String passengerName = passengerNames.get(i);
            String seatNumber = isAssignedSeating ? seatNumbers.get(i) : null;

            Seat seat = isAssignedSeating ?
                findAndValidateAssignedSeat(scheduleSeats, seatNumber) :
                findAvailableSeat(scheduleSeats);

            // Create ticket
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setSchedule(schedule);
            ticket.setPassengerName(passengerName);
            ticket.setPassengerEmail(email);
            ticket.setPassengerPhone(phone);
            ticket.setSeatNumber(seat.getSeatNumber());
            ticket.setIsPrimary(i == 0);
            ticket.setCreatedAt(LocalDateTime.now());

            // Mark seat as sold
            seat.setIsSold(true);
            seatRepository.save(seat);

            // Add ticket to list (will be persisted when booking is saved)
            tickets.add(ticket);
        }

        // Set the bidirectional relationship
        booking.setTickets(tickets);
        ticketRepository.saveAll(tickets);

        // eagerly fetch the schedule + class‐prices
        Schedule fullSchedule = scheduleRepository.findByIdWithPrices(scheduleId)
            .orElseThrow(() -> new EntityNotFoundException("Schedule not found: " + scheduleId));
        // replace the booking’s schedule reference
        booking.getTickets().forEach(t -> t.setSchedule(fullSchedule));

        // now booking.getTickets().get(0).getSchedule().getScheduleClassPrices() is safe
        //return bookingRepository.save(booking);
        return booking;
    }

    private Seat findAndValidateAssignedSeat(List<Seat> seats, String seatNumber) {
        return seats.stream()
            .filter(s -> s.getSeatNumber().equalsIgnoreCase(seatNumber))
            .findFirst()
            .map(seat -> {
                if (Boolean.TRUE.equals(seat.getIsSold())) {
                    throw new IllegalStateException("Seat already sold: " + seatNumber);
                }
                return seat;
            })
            .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + seatNumber));
    }

    private Seat findAvailableSeat(List<Seat> seats) {
        return seats.stream()
            .filter(s -> Boolean.FALSE.equals(s.getIsSold()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No available seats"));
    }
}
