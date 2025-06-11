package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.service.BusLayoutService;
import com.tazifor.busticketing.util.BeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tazifor.busticketing.service.Screen.STEP_CHOOSE_SEAT;

@Component("DISPLAY_RESULTS")
public class DisplayResultsHandler implements ScreenHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(DisplayResultsHandler.class);
    @Override
    public ScreenHandlerResult handleDataExchange(FlowDataExchangePayload payload,
                                                  BookingState state) {

        // 2) Advance our state machine to CHOOSE_SEAT
        Map<String, Object> data = payload.getData();

        BookingState newState = state.withTime(data.get("time").toString())
            .withTravelClass(data.get("class").toString())
            .withAgency(data.get("agency").toString())
            .withPrice(data.get("price").toString())
            .withStep(STEP_CHOOSE_SEAT);

        // 3) Fetch the cached bus image + seat coordinates from BusLayoutService
        BusLayoutService layoutService = BeanUtil.getBean(BusLayoutService.class);
        String busBase64 = layoutService.getBase64BusImage(); // “responseData:image/png;base64,…”
        Map<String, Point> seatCoords = layoutService.getSeatCoordinates();

        if (busBase64 == null || seatCoords == null || seatCoords.isEmpty()) {
            // If for some reason the service didn’t produce an image or any seats
            Map<String,Object> err = Map.of(
                "error_message", "🚧 Unable to load seat map right now."
            );
            return new ScreenHandlerResult(newState, new NextScreenResponsePayload(STEP_CHOOSE_SEAT, err));
        }

        // 4) Build the “seats” list dynamically from seatCoords.keySet()
        //    Each entry is a Map: { "id": seatId, "title": seatId, "on-select-action": { … } }
        List<Map<String,Object>> seats = seatCoords.keySet().stream()
            .sorted() // optional: sort seat IDs lexicographically (A1, A2, A3, … B1, B2, …)
            .map(seatId -> Map.<String,Object>of(
                "id", seatId,
                "title", seatId,
                // on-select-action must have an empty payload so Flow doesn’t reject
                "on-select-action", Map.of(
                    "name",    "update_data",
                    "enabled", true,
                    "payload", Map.of()  // payload is intentionally empty
                )
            ))
            .collect(Collectors.toList());

        // 4b) Put into a single responseData map
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("image", busBase64);
        fields.put("seats", seats);


        // 5) Return payload whose `responseData` matches your JSON layout’s placeholders
        NextScreenResponsePayload nextScreenResponsePayload = new NextScreenResponsePayload(STEP_CHOOSE_SEAT, fields);
        return new ScreenHandlerResult(newState, nextScreenResponsePayload);
    }
}
