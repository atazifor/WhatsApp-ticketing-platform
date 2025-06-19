package com.nourri.busticketing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.nourri.busticketing.handler.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private static final String VERIFY_TOKEN = "zap_verify";
    private final WebhookDispatcher dispatcher;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge
    ) {
        logger.info("Verify webhook called ");
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    /**
     * Handle messages, flow_action, message statuses
     * */
    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody JsonNode payload) {
        logger.info(" ==== Received webhook: === \n{}\n =====", payload);
        dispatcher.dispatch(payload);

        return ResponseEntity.ok().build();
    }

}
