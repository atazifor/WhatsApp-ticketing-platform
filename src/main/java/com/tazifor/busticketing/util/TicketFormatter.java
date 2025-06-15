package com.tazifor.busticketing.util;

import com.tazifor.busticketing.model.Ticket;

public class TicketFormatter {
    public static String formatTicketCaption(Ticket t) {
        String rtLine = t.isRoundTrip()
            ? "🔁 Round‑trip booked — call " + t.getAgency() + " at " + t.getAgencyPhone() + " for return arrangements."
            : "";

        return """
        🎟️ Ticket: %s
        👤 Passenger: %s
        🚍 Agency: %s | 📞 %s
        🛣️ Route: %s → %s
        📅 Date: %s   ⏰ Time: %s
        💺 Seat: %s   | Class: %s
        💰 Price: %d FCFA

        %s
        _Please arrive 15 minutes early._
        """.formatted(
            t.getTicketNumber(),
            t.getPassengerName(),
            t.getAgency(),
            t.getAgencyPhone(),
            t.getOrigin(),
            t.getDestination(),
            t.getDate(),
            t.getTime(),
            t.getSeat() == null ? "Open" : t.getSeat(),
            t.getTravelClass(),
            t.getPrice(),
            rtLine
        );
    }
}
