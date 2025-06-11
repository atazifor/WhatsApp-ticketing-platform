package com.tazifor.busticketing.service.ui;

import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.util.encoding.BookingStateCodec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TripScheduleCardBuilder {

    public List<Map<String, Object>> build(List<AgencySchedule> allSchedules, String date, BookingState state) {
        return allSchedules.stream()
            .map(schedule -> Map.of(
                "id", schedule.agency() + "_" + schedule.travelClass() + "_" + schedule.time(),
                "main-content", Map.of(
                    "title", truncate(schedule.agency() + " - " + schedule.travelClass(), 30),
                    "metadata", truncate(
                        capitalize(schedule.from()) + " → " + capitalize(schedule.to()) +
                            " | " + schedule.time() +
                            " | " + formatPrice(schedule.price()),
                        80
                    )
                ),
                "on-click-action", Map.of(
                    "name", "data_exchange",
                    "payload", Map.of(
                        "screen", "CHOOSE_SEAT",
                        "agency", schedule.agency(),
                        "origin", schedule.from(),
                        "destination", schedule.to(),
                        "class", schedule.travelClass(),
                        "date", date,
                        "time", schedule.time(),
                        "price", schedule.price(),
                        "_state", BookingStateCodec.encode(state)
                    )
                )
            ))
            .toList();
    }

    private String formatPrice(int price) {
        return String.format("%,dF", price);
    }

    private String truncate(String str, int limit) {
        return (str.length() <= limit) ? str : str.substring(0, limit - 1) + "…";
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}

