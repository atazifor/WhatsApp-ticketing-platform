package com.tazifor.busticketing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tazifor.busticketing.util.encoding.BookingStateCodec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * When you decrypt a request and decide “the next screen is X with data Y,”
 * build one of these and serialize it to a Map; then we encrypt the resulting JSON.
 *
 * Plain text (before encryption) example:
 * {
 *   "screen": "CHOOSE_DATE",
 *   "data": {
 *     "destination": "new_york",
 *     "dates": [ { "id":"2025-06-10","title":"Tue Jun 10 2025" } … ]
 *   }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class NextScreenResponsePayload implements FlowResponsePayload {

    /**
     * Name of the screen to render next.
     * This must correspond to a screen defined in the Flow JSON layout.
     */
    private String screen;

    /**
     * Arbitrary key-value data passed to the screen.
     * You may include an "error_message" key to indicate validation errors.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> data;

    public static NextScreenResponsePayload of(String screen, Map<String, Object> data) {
        return new NextScreenResponsePayload(screen, data);
    }

    public NextScreenResponsePayload withState(BookingState state) {
        Map<String, Object> newData = new HashMap<>(data);
        newData.put("_state", BookingStateCodec.encode(state));
        return new NextScreenResponsePayload(screen, newData);
    }
}
