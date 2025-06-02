package com.tazifor.busticketing.controller;

import com.tazifor.busticketing.dto.crypto.FlowEncryptedPayload;
import com.tazifor.busticketing.dto.error.ErrorNotificationRequest;
import com.tazifor.busticketing.dto.error.ErrorNotificationResponse;
import com.tazifor.busticketing.service.FlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<String>> handleDataExchange(
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


}
