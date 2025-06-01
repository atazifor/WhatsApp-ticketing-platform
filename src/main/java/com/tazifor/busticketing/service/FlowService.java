package com.tazifor.busticketing.service;


import com.tazifor.busticketing.client.WhatsAppApiClient;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.FinalScreenResponsePayload;
import com.tazifor.busticketing.dto.crypto.FlowEncryptedPayload;
import com.tazifor.busticketing.model.BookingState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates both encrypted (endpoint‐powered) and unencrypted (builder‐only) Flows.
 */
@Service
@RequiredArgsConstructor
public class FlowService {
    private static final Logger logger = LoggerFactory.getLogger(FlowService.class);

    private final EncryptionService encryptionService;
    private final WhatsAppApiClient apiClient;
    private final ObjectMapper objectMapper;

    //
    // In‐memory store for unencrypted (builder‐only) flow state, keyed by flow_token.
    // In production you might use Redis or a database instead.
    //
    private final Map<String, BookingState> plainStateStore = new ConcurrentHashMap<>();

    //
    // If you have a single Flow ID for unencrypted flows, configure it here.
    // (For endpoint‐powered flows, the incoming payload already includes flow_id metadata.)
    //
    private static final String BUILDER_FLOW_ID = "YOUR_UNENCRYPTED_FLOW_ID";


    //===========================================================================
    // PART 1: Encrypted, Endpoint‐Powered Flow
    //===========================================================================

    /**
     * Decrypts the incoming encrypted payload (FlowEncryptedPayload), runs flow logic, re‐encrypts the new state,
     * and returns a Map suitable for serialization back to WhatsApp’s Flow data‐exchange protocol.
     *
     * Endpoint path: POST /webhook/flow/data-exchange
     *
     * @param encryptedPayload the raw encrypted input from WhatsApp
     * @return a Mono emitting the full response map (with keys "version", "flow_token", "data", "screen", and optional "close")
     */
    public Mono<Map<String, Object>> handleExchange(FlowEncryptedPayload encryptedPayload) {
        return Mono.fromCallable(() -> {
            // 1) Decrypt incoming payload (AES data + RSA‐wrapped key + IV)
            EncryptionService.DecryptionResult dr = encryptionService.decryptPayload(
                    encryptedPayload.getEncryptedFlowData(),
                    encryptedPayload.getEncryptedAesKey(),
                    encryptedPayload.getInitialVector()
            );

            // 2) Parse decrypted JSON into a typed request
            FlowDataExchangePayload request = objectMapper.readValue(
                    dr.getClearJson(),
                    FlowDataExchangePayload.class
            );

            // 3) Rebuild or initialize the domain state
            BookingState state = rebuildState(request);

            // 4) Decide which UI screen to show next (or final)
            FlowResponsePayload ui;
            switch (request.getAction()) {
                case "INIT":
                    ui = showInitialScreen(state);
                    break;
                case "BACK":
                    ui = showBackScreen(state);
                    break;
                case "data_exchange":
                    ui = processSubmission(request, state);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + request.getAction());
            }

            // 5) If it’s a final payload, ensure flow_token is included in the ExtensionMessageResponse
            if (ui instanceof FinalScreenResponsePayload finalUi) {
                finalUi.getData().getParams().put("flow_token", request.getFlow_token());
                finalUi.getData().validate();
            }

            // 6) Serialize and re‐encrypt the new state
            String newStateJson = objectMapper.writeValueAsString(state);
            String encryptedState = encryptionService.encryptState(
                    newStateJson, dr.getAesKey(), dr.getIv()
            );

            // 7) Build the response map expected by WhatsApp Flow endpoint
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("version", "3.0");
            response.put("flow_token", request.getFlow_token());
            response.put("data", encryptedState);
            response.put("screen", ui);
            if (ui instanceof FinalScreenResponsePayload) {
                response.put("close", true);
            }
            return response;
        });
    }


    //===========================================================================
    // PART 2: Unencrypted (“Builder‐Only”) Flow
    //===========================================================================

    /**
     * Processes an intermediate (unencrypted, builder‐only) Flow callback coming from nfm_reply.response_json.
     * The request already contains { version, action, screen, data, flow_token, etc. } in plain JSON.
     *
     * Called from MessageHandler when interactive.type == "nfm_reply" and JSON has version/action.
     *
     * @param request the parsed FlowDataExchangePayload
     * @return a Mono emitting a Map that represents exactly the Cloud API /messages payload
     *         (with "messaging_product", "to", "type", and "interactive").
     *
     * That Map will be POSTed to /vX.X/{PHONE_NUMBER_ID}/messages by MessageHandler.
     */
    public Mono<Map<String, Object>> handlePlainExchange(FlowDataExchangePayload request) {
        return Mono.fromCallable(() -> {
            String flowToken = request.getFlow_token();

            // 1) Retrieve or create the BookingState for this flow_token
            BookingState state = plainStateStore.computeIfAbsent(flowToken, t -> new BookingState());

            // 2) Initialize or update state based on request.getAction()
            if ("INIT".equals(request.getAction())) {
                logger.info("Plain Flow INIT for token {}", flowToken);
                state.setStep("choose_destination");

            } else if ("data_exchange".equals(request.getAction())) {
                String currentStep = state.getStep();
                switch (currentStep) {
                    case "choose_destination" -> {
                        // In a builder‐only Flow, field keys look like "screen_0_Name_0"
                        Object destObj = request.getData().get("screen_0_Name_0");
                        String destination = destObj != null ? destObj.toString() : "";
                        state.setDestination(destination);
                        state.setStep("choose_date");
                        logger.info("User chose destination: {} on token {}", destination, flowToken);
                    }
                    case "choose_date" -> {
                        Object dateObj = request.getData().get("screen_0_DoB_1");
                        String date = dateObj != null ? dateObj.toString() : "";
                        state.setDate(date);
                        state.setStep("confirm");
                        logger.info("User chose date: {} on token {}", date, flowToken);
                    }
                    case "confirm" -> {
                        state.setStep("completed");
                        logger.info("User confirmed booking for token {}", flowToken);
                    }
                    default -> throw new IllegalStateException("Unexpected step: " + currentStep);
                }
            }

            // 3) Build the next interactive payload based on the updated state
            Map<String, Object> interactive;
            switch (state.getStep()) {
                case "choose_destination" -> interactive = Map.of(
                        "type", "flow",
                        "body", Map.of("text", "Select your destination:"),
                        "action", Map.of(
                                "name", "flow",
                                "parameters", Map.of(
                                        "flow_message_version", "3.0",
                                        "flow_token", flowToken,
                                        "flow_id", BUILDER_FLOW_ID
                                )
                        )
                );
                case "choose_date" -> interactive = Map.of(
                        "type", "flow",
                        "body", Map.of("text", "Select your travel date:"),
                        "action", Map.of(
                                "name", "flow",
                                "parameters", Map.of(
                                        "flow_message_version", "3.0",
                                        "flow_token", flowToken,
                                        "flow_id", BUILDER_FLOW_ID,
                                        "flow_action_payload", Map.of(
                                                "data", Map.of("destination", state.getDestination())
                                        )
                                )
                        )
                );
                case "confirm" -> interactive = Map.of(
                        "type", "flow",
                        "body", Map.of("text",
                                "Confirm booking for " + state.getDestination()
                                        + " on " + state.getDate() + "?"),
                        "action", Map.of(
                                "name", "flow",
                                "parameters", Map.of(
                                        "flow_message_version", "3.0",
                                        "flow_token", flowToken,
                                        "flow_id", BUILDER_FLOW_ID,
                                        "flow_action_payload", Map.of(
                                                "data", Map.of(
                                                        "destination", state.getDestination(),
                                                        "date", state.getDate()
                                                )
                                        )
                                )
                        )
                );
                default -> throw new IllegalStateException("Invalid step for plain exchange: " + state.getStep());
            }

            // 4) Build full Cloud API /messages payload
            Map<String, Object> responseBody = Map.of(
                    "messaging_product", "whatsapp",
                    "type", "interactive",
                    "interactive", interactive
            );
            return responseBody;
        });
    }

    /**
     * Called when an unencrypted, builder‐only Flow reaches its final “Complete” action.
     * You receive whatever arbitrary JSON was defined in the Complete action under response_json.
     *
     * @param finalParams Map of final fields, e.g. { "flow_token": "...", "appointment_date": "...", etc. }
     * @param from        The user’s phone number (no “+”), so you can send a confirmation message.
     */
    public void handlePlainCompletion(Map<String, Object> finalParams, String from) {
        String flowToken = finalParams.getOrDefault("flow_token", "").toString();
        logger.info("Plain Flow completed for token {} from {} with params {}", flowToken, from, finalParams);

        // Perform any business logic here (e.g. persist appointment to DB)

        // Send a simple text confirmation (you could also send a template)
        String confirmationText = "✅ Booking confirmed! Details: " + formatParams(finalParams);
        apiClient.sendText(from, confirmationText)
                .subscribe(
                        __ -> logger.info("Sent completion message to {}", from),
                        err -> logger.error("Error sending completion message to {}: {}", from, err.getMessage())
                );

        // Clean up in‐memory state
        plainStateStore.remove(flowToken);
    }


    //===========================================================================
    // PART 3: Shared Helper Methods for Encrypted Flows
    //===========================================================================

    /** Rebuilds or initializes your business state from the encrypted FlowDataExchangePayload. */
    private BookingState rebuildState(FlowDataExchangePayload req) {
        BookingState state = new BookingState();
        state.setStep(req.getScreen());
        Object dest = req.getData().get("destination");
        if (dest instanceof String) {
            state.setDestination((String) dest);
        }
        Object date = req.getData().get("date");
        if (date instanceof String) {
            state.setDate((String) date);
        }
        return state;
    }

    /** Builds the very first screen of an encrypted flow. */
    private NextScreenResponsePayload showInitialScreen(BookingState state) {
        state.setStep("choose_destination");
        return new NextScreenResponsePayload(
                "choose_destination",
                Map.of(
                        "prompt", "Select your destination",
                        "options", Map.of(
                                "new_york", "New York",
                                "boston", "Boston"
                        )
                )
        );
    }

    /** Handles “Back” by re‐showing the previous screen (stubbed as re‐initializing). */
    private NextScreenResponsePayload showBackScreen(BookingState state) {
        return showInitialScreen(state);
    }

    /**
     * Processes a user submission from an encrypted flow screen and returns either the next screen
     * (NextScreenResponsePayload) or a final success (FinalScreenResponsePayload).
     */
    private FlowResponsePayload processSubmission(FlowDataExchangePayload req, BookingState state) {
        switch (state.getStep()) {
            case "choose_destination" -> {
                state.setStep("choose_date");
                return new NextScreenResponsePayload(
                        "choose_date",
                        Map.of("prompt", "Select travel date")
                );
            }
            case "choose_date" -> {
                state.setStep("confirm");
                return new NextScreenResponsePayload(
                        "confirm_booking",
                        Map.of(
                                "destination", state.getDestination(),
                                "date", state.getDate(),
                                "message", "Confirm booking?"
                        )
                );
            }
            case "confirm" -> {
                state.setStep("completed");
                return new FinalScreenResponsePayload(
                        new FinalScreenResponsePayload.ExtensionMessageResponse(
                                Map.of("receipt_id", "TCKT-" + Instant.now().toString())
                        )
                );
            }
            default -> throw new IllegalStateException("Unexpected step: " + state.getStep());
        }
    }

    /**
     * Formats finalParams (excluding flow_token) into a user‐friendly string, e.g. "date=2025-05-31 destination=New York".
     */
    private String formatParams(Map<String, Object> finalParams) {
        Map<String, Object> copy = new java.util.HashMap<>(finalParams);
        copy.remove("flow_token");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : copy.entrySet()) {
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append(" ");
        }
        return sb.toString().trim();
    }
}

