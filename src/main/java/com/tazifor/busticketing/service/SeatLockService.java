package com.tazifor.busticketing.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporarily locks seats per user session (flowToken).
 * In production, you could replace this with Redis or DB-backed locking.
 */
@Service
public class SeatLockService {

    private final Map<String, Set<String>> takenSeatsByFlow = new ConcurrentHashMap<>();

    public Set<String> getTakenSeats(String flowToken) {
        return Collections.unmodifiableSet(
            takenSeatsByFlow.getOrDefault(flowToken, Collections.emptySet())
        );
    }

    public void markSeatTaken(String flowToken, String seatId) {
        takenSeatsByFlow
            .computeIfAbsent(flowToken, k -> ConcurrentHashMap.newKeySet())
            .add(seatId);
    }

    public boolean isSeatTaken(String flowToken, String seatId) {
        Set<String> taken = takenSeatsByFlow.get(flowToken);
        return taken != null && taken.contains(seatId);
    }

    public void clearSeatsForFlow(String flowToken) {
        takenSeatsByFlow.remove(flowToken);
    }
}
