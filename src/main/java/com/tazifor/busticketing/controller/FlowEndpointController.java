package com.tazifor.busticketing.controller;

import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.crypto.FlowEncryptedPayload;
import com.tazifor.busticketing.dto.error.ErrorNotificationRequest;
import com.tazifor.busticketing.dto.error.ErrorNotificationResponse;
import com.tazifor.busticketing.dto.health.HealthCheckRequest;
import com.tazifor.busticketing.dto.health.HealthCheckResponse;
import com.tazifor.busticketing.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/webhook/flow")
@RequiredArgsConstructor
public class FlowEndpointController {
    private final FlowService flowService;

    /**
     * Handles data exchange calls (INIT, BACK, data_exchange).
     * Receives encrypted payload, returns the next or final screen payload.
     */
    @PostMapping("/data-exchange")
    public Mono<ResponseEntity<? extends Map<String, Object>>> handleDataExchange(
            @RequestBody FlowEncryptedPayload request
    ) {
        return flowService
                .handleExchange(request)
                .map(ResponseEntity::ok);
    }


    @PostMapping("/error-notification")
    public ResponseEntity<ErrorNotificationResponse> handleErrorNotification(
            @RequestBody ErrorNotificationRequest r
    ) {
        return ResponseEntity.ok(new ErrorNotificationResponse(new ErrorNotificationResponse.Acknowledgement(true)));
    }
    @PostMapping("/ping")
    public ResponseEntity<HealthCheckResponse> handleHealthCheck(
            @RequestBody HealthCheckRequest r
    ) {
        return ResponseEntity.ok(new HealthCheckResponse(new HealthCheckResponse.HealthStatus("active")));
    }
}
