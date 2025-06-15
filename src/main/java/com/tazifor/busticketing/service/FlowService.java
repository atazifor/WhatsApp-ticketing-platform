package com.tazifor.busticketing.service;

import com.tazifor.busticketing.dto.*;
import com.tazifor.busticketing.dto.crypto.FlowEncryptedPayload;
import com.tazifor.busticketing.model.BookingState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.model.Ticket;
import com.tazifor.busticketing.model.TicketFactory;
import com.tazifor.busticketing.service.screens.ScreenHandler;
import com.tazifor.busticketing.util.encoding.BookingStateCodec;
import com.tazifor.busticketing.util.StateDiffUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates both encrypted (endpoint‐powered) and unencrypted interactive Flows.
 */
@Service
@RequiredArgsConstructor
public class FlowService {
    private static final Logger logger = LoggerFactory.getLogger(FlowService.class);

    private final FlowEncryptionService encryptionService;
    private final Map<String, ScreenHandler>  screenHandlers;
    private final ObjectMapper objectMapper;
    private final TicketSendingService ticketSendingService;

    /**
     * Decrypts the incoming encrypted payload (FlowEncryptedPayload), runs flow logic, re‐encrypts the new state,
     * and returns a Map suitable for serialization back to WhatsApp’s Flow data‐exchange protocol.
     * Endpoint path: POST /webhook/flow/data-exchange
     *
     * @param encryptedPayload the raw encrypted input from WhatsApp
     * @return a Mono emitting the full response map (with keys "version", "flow_token", "data", "screen", and optional "close")
     */
    public Mono<String> handleExchange(FlowEncryptedPayload encryptedPayload) {
        return Mono.fromCallable(() -> {
            // 1) Decrypt incoming payload (AES data + RSA‐wrapped key + IV)
            FlowEncryptionService.DecryptionResult dr = encryptionService.decryptPayload(
                    encryptedPayload.getEncryptedFlowData(),
                    encryptedPayload.getEncryptedAesKey(),
                    encryptedPayload.getInitialVector()
            );

            // 2) Parse decrypted JSON into a typed request
            FlowDataExchangePayload decryptedRequestPayload = objectMapper.readValue(
                    dr.clearJson(),
                    FlowDataExchangePayload.class
            );

            String action = decryptedRequestPayload.getAction();
            logger.info("Flow action {}", action);

            // 3) If "ping", return encrypted health‐check
            if ("ping".equals(action)) {
                String healthJson = objectMapper.writeValueAsString(
                        Map.of("data", Map.of("status", "active"))
                );
                return encryptionService.encryptPayload(
                        healthJson, dr.aesKey(), dr.iv()
                );
            }

            // 4) Rebuild or initialize the domain state
            BookingState state = Optional.ofNullable(decryptedRequestPayload.getData().get("_state"))
                .map(encoded -> BookingStateCodec.decode(encoded.toString()))
                .orElse(BookingState.empty());

            // 5) Decide which UI screen to show next (or final)
            ScreenHandlerResult screenHandlerResult;
            switch (action) {
                case "INIT":
                    logger.info("INIT for token {}", decryptedRequestPayload.getFlow_token());
                    screenHandlerResult = Screen.buildInitialScreen(state);
                    break;
                case "BACK":
                    screenHandlerResult = Screen.showBackScreen(state);
                    break;
                case "data_exchange":
                    // Look up enum by req.getScreen() and invoke its handle(...)
                    String currentScreen = decryptedRequestPayload.getScreen();
                    logger.info("data_exchange for screen {}", currentScreen);
                    logger.info("decryptedRequestPayload {}", decryptedRequestPayload);

                    if(StringUtils.hasLength(currentScreen) || !screenHandlers.containsKey(currentScreen)){
                        if (decryptedRequestPayload.getData().containsKey("error")) {
                            screenHandlers.get("GENERIC_ERROR");
                        }
                    }

                    ScreenHandler screenHandler = screenHandlers.get(currentScreen);
                    screenHandlerResult = screenHandler.handleDataExchange(decryptedRequestPayload, state);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }

            BookingState newState = screenHandlerResult.newState();
            FlowResponsePayload flowResponsePayload = screenHandlerResult.response();
            // 6) If it’s a final payload, ensure flow_token is included in the ExtensionMessageResponse
            if (flowResponsePayload instanceof FinalScreenResponsePayload finalUi) {
                ((FinalScreenResponsePayload.ExtensionMessageResponse)finalUi.getData().get("extension_message_response")).validate();
            }else {
                flowResponsePayload = ((NextScreenResponsePayload) flowResponsePayload).withState(newState);
            }

            logger.info("State transition: [{}] → [{}]\n{}\nState\n-----\n{}",
                state.getStep(),
                newState.getStep(),
                StateDiffUtil.prettyPrintDiff(state, newState),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newState)
            );

            // 7) Serialize and re‐encrypt the new state
            String uiAsString = objectMapper.writeValueAsString(flowResponsePayload);
            return encryptionService.encryptPayload(uiAsString, dr.aesKey(), dr.iv());

        });
    }

    /**
     * Called when a Flow reaches its final “Complete” action.
     * You receive whatever arbitrary JSON was defined in the Complete action under response_json.
     *
     * @param finalParams Map of final fields, e.g. { "flow_token": "...", "appointment_date": "...", etc. }
     * @param from        The user’s phone number (no “+”), so you can send a confirmation message.
     */
    public void handleFlowCompletion(Map<String, Object> finalParams, String from) {
        String flowToken = finalParams.getOrDefault("flow_token", "").toString();

        logger.info("Plain Flow completed for token {} from {} with params {}", flowToken, from, finalParams);

        // Perform any business logic here (e.g. persist appointment to DB)
        List<Ticket> tickets = TicketFactory.fromFinalParams(finalParams);

        // send tickets
        ticketSendingService.sendAllTickets(from, tickets);
    }

}

