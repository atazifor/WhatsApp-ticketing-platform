package com.tazifor.busticketing.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@ToString
public class SeatingInfo {
    private final boolean openSeating;
    @Getter
    private final int totalSeats;
    private final Map<String, List<Seat>> seatMap; // class -> seats list
    private final Map<String, Integer> seatsSold;  // class -> sold count

    public boolean isOpenSeating() {
        return openSeating;
    }

    public int availableInClass(String className) {
        if (openSeating) {
            int sold = seatsSold.getOrDefault(className, 0);
            return totalSeats - sold;
        } else {
            List<Seat> seats = seatMap.getOrDefault(className, List.of());
            return (int) seats.stream().filter(seat -> !seat.isSold()).count();
        }
    }

    public boolean markSeatAsSold(String className, String seatNumber) {
        if (openSeating) {
            int sold = seatsSold.getOrDefault(className, 0);
            if (sold < totalSeats) {
                seatsSold.put(className, sold + 1);
                return true;
            }
            return false;
        } else {
            List<Seat> seats = seatMap.get(className);
            for (int i = 0; i < seats.size(); i++) {
                Seat seat = seats.get(i);
                if (seat.number().equals(seatNumber) && !seat.isSold()) {
                    seats.set(i, new Seat(seat.number(), true)); // replace with sold seat
                    return true;
                }
            }
            return false;
        }
    }

    public List<Seat> getAvailableSeats(String className) {
        if (openSeating) return List.of(); // open seating has no seat numbers
        return seatMap.getOrDefault(className, List.of()).stream()
            .filter(seat -> !seat.isSold())
            .toList();
    }

    public boolean isSoldOut(String className) {
        return availableInClass(className) == 0;
    }


    public int getSoldCount(String className) {
        if (openSeating) {
            return seatsSold.getOrDefault(className, 0);
        } else {
            return (int) seatMap.getOrDefault(className, List.of()).stream()
                .filter(Seat::isSold)
                .count();
        }
    }

    public List<String> getAllAvailableSeatNumbers(String className) {
        if (openSeating) return List.of();
        return seatMap.getOrDefault(className, List.of()).stream()
            .filter(seat -> !seat.isSold())
            .map(Seat::number)
            .filter(number -> !number.equals("10")) //TODO: remove this in prod
            .toList();
    }

    public int unsoldCount() {
        if (openSeating) {
            return seatsSold.entrySet().stream()
                .mapToInt(entry -> totalSeats - entry.getValue())
                .sum();
        } else {
            return seatMap.values().stream()
                .flatMap(List::stream)
                .mapToInt(seat -> seat.isSold() ? 0 : 1)
                .sum();
        }
    }

    public int unsoldCount(String travelClass) {
        if (openSeating) {
            int sold = seatsSold.getOrDefault(travelClass, 0);
            return totalSeats - sold;
        } else {
            List<Seat> seats = seatMap.getOrDefault(travelClass, List.of());
            return (int) seats.stream().filter(seat -> !seat.isSold()).count();
        }
    }

}

