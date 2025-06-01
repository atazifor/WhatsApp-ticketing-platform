package com.tazifor.busticketing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response sent to WhatsApp after a data exchange request (except the final step).
 *
 * This payload tells the client which screen to render next and what data to use.
 * If an error occurs (e.g. invalid user input), include an optional error_message
 * inside the `data` map. This will display a snackbar error to the user.
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
}
