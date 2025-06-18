package com.tazifor.busticketing.service.ui;

import com.tazifor.busticketing.model.Schedule;
import com.tazifor.busticketing.dto.BookingState;
import com.tazifor.busticketing.model.TravelClass;
import com.tazifor.busticketing.repository.ScheduleClassPriceRepository;
import com.tazifor.busticketing.repository.SeatRepository;
import com.tazifor.busticketing.service.AgencyService;
import com.tazifor.busticketing.util.encoding.BookingStateCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_SEAT;
import static com.tazifor.busticketing.service.Screen.STEP_NUMBER_OF_TICKETS;

@Component
@RequiredArgsConstructor
public class TripScheduleCardBuilder {
    private final static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TripScheduleCardBuilder.class);

    private final SeatRepository seatRepository;
    private final AgencyService agencyService;
    private final ScheduleClassPriceRepository priceRepository;


    public List<Map<String, Object>> build(List<Schedule> allSchedules, String date, BookingState state) {
        return allSchedules.stream()
            .flatMap(schedule -> priceRepository.findBySchedule(schedule).stream()
                .map(priceEntry -> buildCard(schedule, priceEntry.getTravelClass(), priceEntry.getPrice(), state))
            )
            .toList();
    }

    private Map<String, Object> buildCard(Schedule schedule, TravelClass travelClass, int price, BookingState state) {
        Map<String, Object> card = new LinkedHashMap<>();

        String agencyName = schedule.getAgency().getName();
        String className = travelClass.getName();
        String from = schedule.getFromLocation().getName();
        String to = schedule.getToLocation().getName();
        String time = schedule.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String date = schedule.getTravelDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        boolean isOpenSeating = schedule.getBus().getIsOpenSeating();
        int available = seatRepository.countByScheduleIdAndTravelClassIdAndIsSoldFalse(
            schedule.getId(),
            travelClass.getId()
        );

        int maxPerBooking = agencyService.getMaxTicketsPerBooking(agencyName);
        String nextScreen = isOpenSeating ? STEP_NUMBER_OF_TICKETS : STEP_CHOOSE_SEAT;

        // Required fields
        card.put("id", schedule.getId()+"/"+travelClass.getId());
        card.put("main-content", Map.of(
            "title", truncate(agencyName + " - " + className, 30),
            "metadata", truncate(
                capitalize(from) + " → " + capitalize(to) +
                    " | " + time +
                    " | " + formatPrice(price),
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
                card.put("tags", List.of(available + " seat%s left ".formatted(available == 1 ? "" : "s")));
                //card.put("tags", List.of(available + " | " + date));
            }
            card.put("on-click-action", Map.of(
                "name", "data_exchange",
                "payload", Map.of(
                    "screen", nextScreen,
                    "agency", agencyName,
                    "origin", from,
                    "destination", to,
                    "class", className,
                    "date", date,
                    "time", time,
                    "price", price,
                    "_state", BookingStateCodec.encode(state)
                )
            ));
        }

        return card;
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

