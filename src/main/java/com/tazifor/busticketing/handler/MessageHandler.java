package com.tazifor.busticketing.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.service.FlowService;
import com.tazifor.busticketing.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Handles an array of incoming WhatsApp “messages” objects.
 * <ul>
 *      <li>If type="text", either
 *      <ul>
     *      <li> send a simple reply or
     *      <li> send a template message or
     *      <li> send a new Flow.
 *      </ul>
 *      <li>If type="interactive" and interactive.type="nfm_reply", extract the Flow JSON
 *    and pass it to the FlowService for data-exchange logic.
 *    <ul/>
 */
@Service
@RequiredArgsConstructor
public class MessageHandler {
    Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final MessageService messageService;
    private final FlowService flowService;
    private final ObjectMapper objectMapper;

    public void handle(JsonNode messages) {
        for(JsonNode message: messages) {
            String type = message.path("type").asText();
            String from = message.path("from").asText();
            String messageId = message.path("id").asText();
            switch(type) {
                case "text" -> handleTextMessage(message, from);
                case "interactive" -> handleInteractiveMessage(message, from);
                default -> logger.info("Unhandled message type: {}", type);

            }

        }
    }

    private void handleTextMessage(JsonNode message, String from) {
        String body = message.path("text").path("body").asText();
        logger.info("Received text from {} : {}", from, body);
        messageService.processIncomingText(from, body);
    }

    private void handleInteractiveMessage(JsonNode message, String from) {
        JsonNode interactive = message.path("interactive");
        String interactiveType = interactive.path("type").asText();

        if (!"nfm_reply".equals(interactiveType)) {
            logger.info("Received interactive type that is not nfm_reply: {}", interactiveType);
            return;
        }
        // Extract the raw JSON string from response_json
        String responseJson = interactive.path("nfm_reply").path("response_json").asText();
        logger.info("Received unencrypted Flow nfm_reply: {}", responseJson);

        // Parse into a JsonNode to inspect its contents
        JsonNode root;
        try {
            root = objectMapper.readTree(responseJson);
        } catch (Exception e) {
            logger.error("Invalid JSON in nfm_reply.response_json", e);
            return;
        }

        // CASE A: Intermediate Flow screen (builder‐only). Look for "version" and "action".
        if (root.has("version") && root.has("action")) {
            FlowDataExchangePayload midRequest;
            try {
                midRequest = objectMapper.treeToValue(root, FlowDataExchangePayload.class);
            } catch (Exception e) {
                logger.error("Failed to map intermediate Flow payload to FlowDataExchangePayload", e);
                return;
            }

            // Process intermediate screen: this returns a Map representing the next Flow message.
            flowService.handlePlainExchange(midRequest)
                .flatMap(responseBodyMap -> {
                    // Send the next Flow screen back via Cloud API
                    return messageService.sendRawFlowMessage(from, responseBodyMap);
                })
                .subscribe(
                    __ -> logger.info("Sent next Flow screen (unencrypted) to {}", from),
                    err -> logger.error("Error sending next Flow screen", err)
                );
            return;
        }
        // CASE B: Final/Completion of a builder‐only (unencrypted) Flow
        //   Here root does NOT have "version"/"action"—it’s whatever you put in your Complete action.
        Map<String, Object> finalParams;
        try {
            finalParams = objectMapper.convertValue(root, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Failed to convert final Flow payload to Map<String, Object>", e);
            return;
        }

        logger.info("Handling Flow completion with params: {}", finalParams);
        // Pass finalParams (e.g. {flow_token: "...", arrival_date: "...", ...}) to your completion logic
        flowService.handlePlainCompletion(finalParams, from);

    }
}
