package com.tazifor.busticketing.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.tazifor.busticketing.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatusHandler {
    Logger logger = LoggerFactory.getLogger(StatusHandler.class);
    private final MessageService messageService;


    public void handle(JsonNode statuses) {
        for(JsonNode status: statuses) {
            String messageId = status.path("id").asText();
            String recipientId = status.path("recipient_id").asText();
            String statusType = status.path("status").asText();
            long timestamp = status.path("timestamp").asLong();
            String category = status.path("pricing").path("category").asText();
            logger.info("Status update for message {} to {}: status={} at {} (category={})",
                    messageId, recipientId, statusType, timestamp, category);
        }
    }
}
