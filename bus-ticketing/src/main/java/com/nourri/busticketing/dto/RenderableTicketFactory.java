package com.nourri.busticketing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

public class RenderableTicketFactory {
    private final static Logger logger = LoggerFactory.getLogger(RenderableTicketFactory.class);
    final static ObjectMapper MAPPER = new ObjectMapper();
    public static List<RenderableTicket> fromFinalParams(Map<String, Object> finalParams) {
        List<String> seats = Optional.ofNullable((List<String>) finalParams.get("seat")).orElse(List.of());

        List<Object> rawList = Optional.ofNullable((List<Object>) finalParams.get("passengers"))
            .orElse(List.of());

        List<Passenger> passengers = rawList.stream()
            .filter(Map.class::isInstance)
            .map(obj -> MAPPER.convertValue(obj, Passenger.class))
            .toList();

        logger.info("Creating tickets for {} passengers", passengers);

        boolean isRoundTrip = Boolean.parseBoolean(finalParams.getOrDefault("is_round_trip", "false").toString());
        int basePrice = Integer.parseInt(finalParams.get("price").toString());
        int price = isRoundTrip ? (int)(basePrice * 2 * 0.85) : basePrice;

        List<RenderableTicket> tickets = new ArrayList<>();
        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            RenderableTicket t = new RenderableTicket();
            t.setTicketNumber("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            t.setPassengerName(p.getName());
            t.setPassengerEmail(p.getEmail());
            t.setPassengerPhone(p.getPhone());
            t.setSeat(i < seats.size() ? seats.get(i) : null);
            t.setOrigin(finalParams.get("origin").toString());
            t.setDestination(finalParams.get("destination").toString());
            t.setDate(finalParams.get("date").toString());
            t.setTime(finalParams.get("time").toString());
            t.setTravelClass(finalParams.get("class").toString());
            t.setAgency(finalParams.get("agency").toString());
            t.setAgencyPhone(finalParams.get("agency_phone").toString());
            t.setPrice(price);
            t.setRoundTrip(isRoundTrip);
            t.setIssuedAt(LocalDateTime.now());
            tickets.add(t);
        }
        return tickets;
    }
}

