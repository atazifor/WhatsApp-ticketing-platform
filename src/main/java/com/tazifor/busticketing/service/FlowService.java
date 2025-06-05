package com.tazifor.busticketing.service;


import com.tazifor.busticketing.client.WhatsAppApiClient;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.dto.FinalScreenResponsePayload;
import com.tazifor.busticketing.dto.crypto.FlowEncryptedPayload;
import com.tazifor.busticketing.model.BookingState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.service.screens.ScreenHandler;
import com.tazifor.busticketing.util.ImageOverlayUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates both encrypted (endpoint‐powered) and unencrypted interactive Flows.
 */
@Service
@RequiredArgsConstructor
public class FlowService {
    private static final Logger logger = LoggerFactory.getLogger(FlowService.class);

    private final FlowEncryptionService encryptionService;
    private final Map<String, ScreenHandler>  screenHandlers;
    private final WhatsAppApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final ImageOverlayUtil imageOverlayUtil;

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
            BookingState state = rebuildState(decryptedRequestPayload);

            // 5) Decide which UI screen to show next (or final)
            FlowResponsePayload ui;
            switch (action) {
                case "INIT":
                    logger.info("INIT for token {}", decryptedRequestPayload.getFlow_token());
                    ui = buildInitialScreen(state);
                    break;
                case "BACK":
                    ui = showBackScreen(state);
                    break;
                case "data_exchange":
                    // Look up enum by req.getScreen() and invoke its handle(...)
                    String currentScreen = decryptedRequestPayload.getScreen();
                    logger.info("data_exchange for screen {} with payload {}", currentScreen, decryptedRequestPayload);
                    ScreenHandler screenHandler = screenHandlers.get(currentScreen);
                    ui = screenHandler.handleDataExchange(decryptedRequestPayload, state);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }

            // 6) If it’s a final payload, ensure flow_token is included in the ExtensionMessageResponse
            if (ui instanceof FinalScreenResponsePayload finalUi) {
                logger.info("Final UI data {}", finalUi.getData());
                logger.info("Decrypted request payload: {}", decryptedRequestPayload);
                //finalUi.getData().getParams().put("flow_token", decryptedRequestPayload.getFlow_token());
                ((FinalScreenResponsePayload.ExtensionMessageResponse)finalUi.getData().get("extension_message_response")).validate();
            }

            // 7) Serialize and re‐encrypt the new state
            String uiAsString = objectMapper.writeValueAsString(ui);
            logger.info("UI data {}", uiAsString);
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

        // (3) Generate the overlaid PNG as a Base64 string
        //     We assume you stored chosenSeats into your BookingState earlier:
        List<String> chosenSeats = (List<String>) finalParams.getOrDefault("seat", List.of());
        String overlaidBase64 = imageOverlayUtil.createImageWithHighlights(chosenSeats);

        // Send a simple text confirmation (you could also send a template)
        String confirmationText = "✅ Booking confirmed! Details: \n" + formatParams(finalParams);
        /*apiClient.sendText(from, confirmationText)
                .subscribe(
                        __ -> logger.info("Sent completion message to {}", from),
                        err -> logger.error("Error sending completion message to {}: {}", from, err.getMessage())
                );*/
        apiClient.sendImage(from, overlaidBase64, confirmationText)
            .subscribe(
                __ -> logger.info("Sent final ticket image to {}", from),
                err -> logger.error("Error sending final ticket image to {}: {}", from, err.getMessage())
            );
    }


    /** Rebuilds or initializes business state from the encrypted FlowDataExchangePayload. */
    private BookingState rebuildState(FlowDataExchangePayload payload) {
        BookingState state = new BookingState();
        state.setStep(payload.getScreen());
        if (payload.getData() != null) {
            Map<String,Object> data = payload.getData();
            if (data.containsKey("destination")) state.setDestination(data.get("destination").toString());
            if (data.containsKey("date"))        state.setDate(data.get("date").toString());
            if (data.containsKey("time"))        state.setTime(data.get("time").toString());
            if (data.containsKey("seat"))        state.setChosenSeats((Collection<String>) data.get("seat"));
            if (data.containsKey("full_name"))   state.setFullName(data.get("full_name").toString());
            if (data.containsKey("email"))       state.setEmail(data.get("email").toString());
            if (data.containsKey("phone"))       state.setPhone(data.get("phone").toString());
            if (data.containsKey("num_tickets")) state.setNumTickets(data.get("num_tickets").toString());
            if (data.containsKey("more_details"))state.setMoreDetails(data.get("more_details").toString());
        }
        return state;
    }

    /** Builds the very first screen of an encrypted flow. */
    private NextScreenResponsePayload buildInitialScreen(BookingState state) {

        state.setStep("CHOOSE_DESTINATION");

        Object[] destinations = {
            Map.of("id", "new_york", "title", "New York"),
            Map.of("id", "boston", "title", "Boston"),
            Map.of("id", "washington", "title", "Washington DC"),
            Map.of("id", "philadelphia", "title", "Philadelphia")
        };

        return new NextScreenResponsePayload(
            "CHOOSE_DESTINATION",
            Map.of("destinations", destinations)
        );
    }

    /** Handles “Back” by re‐showing the previous screen (stubbed as re‐initializing). */
    private NextScreenResponsePayload showBackScreen(BookingState state) {
        return buildInitialScreen(state);
    }

    /**
     * Formats finalParams (excluding flow_token) into a user‐friendly string, e.g. "date=2025-05-31 destination=New York".
     */
    private String formatParams(Map<String, Object> summaryData) {
        Object seatObj = summaryData.get("seat");
        String seatDisplay;

        if (seatObj == null) {
            seatDisplay = "Not selected";
        } else if (seatObj instanceof Collection<?>) {
            Collection<?> seats = (Collection<?>) seatObj;
            seatDisplay = seats.isEmpty() ? "Not selected" :
                seats.size() == 1 ? seats.iterator().next().toString() :
                    String.join(", ", seats.stream().map(Object::toString).toList());
        } else {
            seatDisplay = seatObj.toString();
        }
        return "🎫 *Your Ticket Confirmation* 🎫\n\n" +
            "*Name:* " + summaryData.get("full_name") + "\n" +
            "*Email:* " + summaryData.get("email") + "\n" +
            "*Phone:* " + summaryData.get("phone") + "\n\n" +
            "*Destination:* " + summaryData.get("destination") + "\n" +
            "*Date:* " + summaryData.get("date") + "\n" +
            "*Time:* " + summaryData.get("time") + "\n" +
            "*💺 Seat(s):* "         + seatDisplay        + "\n" +
            "*Number of Tickets:* " + summaryData.get("num_tickets") + "\n\n";
    }
}

