package com.tazifor.busticketing.service.ui;

import com.tazifor.busticketing.model.AgencySchedule;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.model.ScheduleDetails;
import com.tazifor.busticketing.model.SeatingInfo;
import com.tazifor.busticketing.service.AgencyMetadataService;
import com.tazifor.busticketing.service.ScheduleQueryService;
import com.tazifor.busticketing.util.encoding.BookingStateCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_SEAT;
import static com.tazifor.busticketing.service.Screen.STEP_NUMBER_OF_TICKETS;

@Component
@RequiredArgsConstructor
public class TripScheduleCardBuilder {
    private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TripScheduleCardBuilder.class);

    private final ScheduleQueryService scheduleQueryService;
    private final AgencyMetadataService agencyMetadataService;

    public List<Map<String, Object>> build(List<AgencySchedule> allSchedules, String date, BookingState state) {
        return allSchedules.stream()
            .map(schedule -> buildCard(schedule, date, state))
            .toList();
    }

    private Map<String, Object> buildCard(AgencySchedule schedule, String date, BookingState state) {
        Map<String, Object> card = new LinkedHashMap<>();

        String agency = schedule.agency();
        String className = schedule.travelClass();
        Optional<ScheduleDetails> optDetails = scheduleQueryService.findScheduleDetails(
            schedule.agency(),
            schedule.from(),
            schedule.to(),
            schedule.travelClass(),
            schedule.time()
        );
        int available = 0;
        String nextScreen = STEP_CHOOSE_SEAT;
        if (optDetails.isPresent()) {
            SeatingInfo seatingInfo = optDetails.get().seatingInfo();
            if(seatingInfo.isOpenSeating()) {
                nextScreen = STEP_NUMBER_OF_TICKETS;
            }
            available = seatingInfo.unsoldCount(schedule.travelClass());
        }

        int maxPerBooking = agencyMetadataService.getConfig(agency).maxTicketsPerBooking();

        // Required fields
        card.put("id", generateId(schedule));
        card.put("main-content", Map.of(
            "title", truncate(agency + " - " + className, 30),
            "metadata", truncate(
                capitalize(schedule.from()) + " → " + capitalize(schedule.to()) +
                    " | " + schedule.time() +
                    " | " + formatPrice(schedule.price()),
                80
            )
        ));

        // Optional: display seat info
        if (available == 0) {
            card.put("tags", List.of("Sold Out"));
            card.put("on-click-action", Map.of(
                "name", "navigate",
                "payload", Map.of()
            ));
        } else {
            if (available < maxPerBooking) {
                card.put("tags", List.of(available + " seats left"));
            }
            card.put("on-click-action", Map.of(
                "name", "data_exchange",
                "payload", Map.of(
                    "screen", nextScreen,
                    "agency", agency,
                    "origin", schedule.from(),
                    "destination", schedule.to(),
                    "class", className,
                    "date", date,
                    "time", schedule.time(),
                    "price", schedule.price(),
                    "_state", BookingStateCodec.encode(state)
                )
            ));
        }

        return card;
    }

    private String generateId(AgencySchedule schedule) {
        return schedule.agency() + "_" + schedule.travelClass() + "_" + schedule.time();
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

