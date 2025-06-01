package com.tazifor.busticketing.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload received from WhatsApp when your server sends an invalid response.
 * This lets you know what went wrong and which screen triggered the error.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorNotificationRequest {

    /**
     * Version of the Flow protocol. Always "3.0".
     */
    private String version;

    /**
     * The flow token representing the session that triggered the error.
     */
    private String flow_token;

    /**
     * The screen name where the invalid response occurred.
     */
    private String screen;

    /**
     * Action being performed when the error happened.
     * Typically "INIT" or "data_exchange".
     */
    private String action;

    /**
     * Contains the error details reported by WhatsApp.
     */
    private ErrorData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        /**
         * Machine-readable error code.
         */
        private String error;

        /**
         * Human-readable description of what went wrong.
         */
        private String error_message;
    }
}
