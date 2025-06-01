package com.tazifor.busticketing.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload you must return to WhatsApp during a health check request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {

    private HealthStatus data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthStatus {
        /**
         * Must be set to "active" to indicate the endpoint is functioning properly.
         */
        private String status;
    }
}
