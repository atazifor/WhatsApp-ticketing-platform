package com.nourri.busticketing.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks taken seats by flowToken.
 *
 * In this example, we keep an in-memory map:
 *   key = flowToken (String)
 *   value = set of seat IDs (e.g. "A1", "B3", …) that have been marked taken for that flow.
 *
 * In a production setting, you’d replace this with a database or external cache.
 */
@Service
public class SeatService {

    /**
     * For each flowToken, store the set of seat IDs already taken.
     */
    private final Map<String, Set<String>> takenSeatsByFlow = new ConcurrentHashMap<>();

    /**
     * Returns an unmodifiable view of the set of seats already taken for the given flowToken.
     * If no seats have been recorded yet, returns an empty set.
     */
    public Set<String> getTakenSeats(String flowToken) {
        return Collections.unmodifiableSet(
            takenSeatsByFlow.getOrDefault(flowToken, Collections.emptySet())
        );
    }

    /**
     * Marks the given seatId as taken for this flowToken.
     * If flowToken has no entry yet, a new Set is created.
     *
     * @param flowToken the unique identifier for this flow/session
     * @param seatId    the seat ID to mark as taken (e.g. "A1")
     */
    public void markSeatTaken(String flowToken, String seatId) {
        // Use computeIfAbsent to initialize a thread-safe Set when first needed
        takenSeatsByFlow
            .computeIfAbsent(flowToken, k -> ConcurrentHashMap.newKeySet())
            .add(seatId);
    }

    /**
     * Checks if a seat is already taken for that flowToken.
     *
     * @param flowToken the flow/session identifier
     * @param seatId    the seat ID to check (e.g. "B3")
     * @return true if already taken, false otherwise
     */
    public boolean isSeatTaken(String flowToken, String seatId) {
        Set<String> taken = takenSeatsByFlow.get(flowToken);
        return taken != null && taken.contains(seatId);
    }

    /**
     * (Optional) Clears all taken seats for a given flowToken.
     * You might call this if the flow is canceled or completes.
     *
     * @param flowToken the flow/session identifier
     */
    public void clearSeatsForFlow(String flowToken) {
        takenSeatsByFlow.remove(flowToken);
    }
}
