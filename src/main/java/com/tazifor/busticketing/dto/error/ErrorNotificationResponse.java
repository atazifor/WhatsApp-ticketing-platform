package com.tazifor.busticketing.dto.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload sent back to WhatsApp to acknowledge the error notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorNotificationResponse {

    private Acknowledgement data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Acknowledgement {
        /**
         * Indicates whether the error has been acknowledged by the backend.
         * Must be set to true.
         */
        private boolean acknowledged;
    }
}
