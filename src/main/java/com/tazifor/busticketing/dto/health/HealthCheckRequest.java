package com.tazifor.busticketing.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload sent periodically by WhatsApp to check if your Flow endpoint is healthy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckRequest {

    /**
     * Version of the Flow protocol. Always "3.0".
     */
    private String version;

    /**
     * Must be set to "ping" to indicate a health check.
     */
    private String action;
}
