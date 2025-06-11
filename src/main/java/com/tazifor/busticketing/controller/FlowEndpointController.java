package com.tazifor.busticketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.dto.crypto.FlowEncryptedPayload;
import com.tazifor.busticketing.service.FlowService;
import com.tazifor.busticketing.util.encoding.SignatureValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/webhook/flow")
@RequiredArgsConstructor
public class FlowEndpointController {
    private final static Logger logger = LoggerFactory.getLogger(FlowEndpointController.class);

    private final FlowService flowService;
    // for deserializing raw JSON into FlowEncryptedPayload
    private final ObjectMapper objectMapper;

    private final SignatureValidator signatureValidator;

    /**
     * Handles data exchange calls (INIT, BACK, data_exchange, ping).
     * Receives encrypted payload, returns the next or final screen payload.
     */
    @PostMapping("/data-exchange")
    public Mono<ResponseEntity<String>> handleDataExchange(
            ServerHttpRequest request,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader
    ) {
        // 1) Join all the DataBuffers to a single DataBuffer so we can pull out the raw body bytes
        return DataBufferUtils.join(request.getBody())
            .flatMap(dataBuffer -> {
                byte[] rawBytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(rawBytes);
                DataBufferUtils.release(dataBuffer);

                String rawBody = new String(rawBytes, StandardCharsets.UTF_8);

                // 2) Verify HMAC-SHA256 against “sha256=<hex>”
                if (!signatureValidator.isSignatureValid(rawBody, signatureHeader)) {
                    // 432 = signature mismatch
                    return Mono.just(ResponseEntity.status(432).build());
                }

                // 3) If signature is OK, attempt to parse JSON into FlowEncryptedPayload
                FlowEncryptedPayload payload;
                try {
                    payload = objectMapper.readValue(rawBody, FlowEncryptedPayload.class);
                } catch (Exception e) {
                    // Malformed JSON → 400
                    return Mono.just(ResponseEntity.badRequest().build());
                }

                // 4) Delegate to your existing flowService
                return flowService
                    .handleExchange(payload)
                    .map(ResponseEntity::ok);
            });
    }
}
