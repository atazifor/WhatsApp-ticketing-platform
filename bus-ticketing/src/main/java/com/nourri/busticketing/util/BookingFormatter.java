package com.nourri.busticketing.util;

import com.nourri.busticketing.dto.BookingState;
import com.nourri.busticketing.dto.Passenger;

import java.util.List;
import java.util.Optional;

public class BookingFormatter {

    public static String formatAppointment(String origin, String destination, String date, String time) {
        return "From %s -> %s on %s at %s".formatted(
            origin, destination, date, time
        );
    }

    public static String formatDetails(String name, String email, String phone, String moreDetails) {
        return "Name: %s%nEmail: %s%nPhone: %s%n\"%s\"".formatted(
            name,
            email,
            phone,
            moreDetails == null ? "" : moreDetails
        );
    }

    public static String formatPassengerSummary(List<Passenger> passengers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            sb.append("**Passenger ").append(i + 1).append(":** ");
            sb.append(p.getName());

            if (i == 0) {
                sb.append(" (Primary Contact)\n");
                sb.append("Email: ").append(p.getEmail()).append("\n");
                sb.append("Phone: ").append(p.getPhone()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String buildSummaryText(BookingState state) {
        String appointment = formatAppointment(state.getOrigin(), state.getDestination(), state.getDate(), state.getTime());
        List<String> seats = state.getChosenSeats();
        String seatDisplay = seats.isEmpty() ? "Not selected" : String.join(", ", seats);

        int numTickets = Integer.parseInt(state.getNumTickets());
        int pricePerTicket = Integer.parseInt(state.getPrice());
        boolean isRoundTrip = state.isRoundTrip();

        int baseTotal = pricePerTicket * numTickets;
        String pricingDetails;
        if (isRoundTrip) {
            int roundTotal = baseTotal * 2;
            int discounted = (int)(roundTotal * 0.85);
            pricingDetails = String.format(
                "**Price:** ~~%d FCFA~~ %d FCFA _(15%% off for round-trip)_\n" +
                    "**(Each way):** %d FCFA × %d tickets",
                roundTotal, discounted, pricePerTicket, numTickets
            );
        } else {
            pricingDetails = String.format("**Price:** %d FCFA (%d tickets × %d)", baseTotal, numTickets, pricePerTicket);
        }

        String passengerBlock = formatPassengerSummary(state.getPassengerList());

        return """
        🎫 **Booking Summary**
        ----------------------------
        **Route:** %s
        **Schedule:** %s | %s
        %s
        ----------------------------
        **Agency:** %s | **Class:** %s
        **Seats:** %s
        **Tickets:** %d
        %s
        ----------------------------
        _Additional Info:_ %s
        """.formatted(
            appointment,
            state.getDate(), state.getTime(),
            passengerBlock,
            state.getAgency(),
            state.getTravelClass(),
            seatDisplay,
            numTickets,
            pricingDetails,
            Optional.ofNullable(state.getMoreDetails()).orElse("")
        );
    }


    public static List<String> extractList(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(raw.toString());
    }
}
