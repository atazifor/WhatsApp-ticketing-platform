package com.nourri.busticketing.handler;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Examines the incoming WhatsApp Cloud API webhook payload and routes:
 *  *  - "messages" → MessageHandler
 *  *  - "statuses" → StatusHandler
 */
@Service
@RequiredArgsConstructor
public class WebhookDispatcher {
    Logger logger = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final MessageHandler messageHandler;
    private final StatusHandler statusHandler;

    /**
     * Called by WebhookController whenever Meta pushes a webhook event.
     * This only handles "messages" and "statuses" keys under entry/changes/value.
     */
    public void dispatch(JsonNode node) {
        JsonNode value = node.at("/entry/0/changes/0/value");
        if(value.has("messages")) {
            JsonNode messages = value.get("messages");
            logger.info("Incoming messages payload: {}", messages);
            messageHandler.handle(messages);
        }
        if(value.has("statuses")) {
            JsonNode statuses = value.get("statuses");
            logger.info("Incoming statuses payload: {}", statuses);
            statusHandler.handle(statuses);
        }
    }
}
