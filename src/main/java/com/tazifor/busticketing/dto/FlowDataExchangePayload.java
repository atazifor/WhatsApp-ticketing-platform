package com.tazifor.busticketing.dto;

import lombok.Data;

import java.util.Map;

/**
 * DTO representing the decrypted payload of a WhatsApp Flow Data Exchange request.
 * <p>
 * This object is used after decrypting the encrypted webhook body received from
 * WhatsApp's Flow API. It provides the necessary context and user input data
 * needed to determine the next screen or final state in a Flow.
 * </p>
 *
 * <p>
 * The Flow Data Exchange request is triggered by WhatsApp when the user:
 * <ul>
 *     <li>Checks the health of the configured endpoint url for the flow (action = {@code ping})</li>
 *     <li>Initiates a Flow (action = {@code INIT})</li>
 *     <li>Presses the Back button (action = {@code BACK})</li>
 *     <li>Submits a screen (action = {@code data_exchange})</li>
 * </ul>
 * </p>
 * Example decrypted payload:
 * <pre>
 * {
 *   "version": "3.0",
 *   "action": "data_exchange",
 *   "screen": "choose_destination",
 *   "data": {
 *     "destination": "new_york"
 *   },
 *   "flow_token": "a6f1c2b3..."
 * }
 * </pre>
 */
@Data
public class FlowDataExchangePayload {
    /**
     * The version of the Flow protocol.
     * <p>Always expected to be set to "3.0".</p>
     * <p><b>Required.</b></p>
     */
    private String version;

    /**
     * The type of interaction that triggered this request.
     * <p>Possible values include:
     * <ul>
     *     <li>{@code INIT} – Flow just started</li>
     *     <li>{@code BACK} – User pressed back</li>
     *     <li>{@code data_exchange} – User submitted a screen</li>
     * </ul>
     * </p>
     * <p><b>Required.</b></p>
     */
    private String action;

    /**
     * The name of the screen the user is interacting with.
     * <p>
     * Required for {@code data_exchange} actions, optional for {@code INIT} and {@code BACK}.
     * The name "SUCCESS" is reserved and cannot be used.
     * </p>
     */
    private String screen;

    /**
     * The user-submitted form data or key-value context passed from the screen.
     * <p>
     * This is a flexible structure and can contain strings, booleans, numbers, arrays, or nested objects.
     * Only populated for {@code data_exchange} actions.
     * </p>
     * <p><b>Required if {@code action = data_exchange}.</b></p>
     */
    private Map<String, Object> data;

    /**
     * A unique token identifying the Flow session.
     * <p>
     * This token is initially generated and sent by the business in the Flow message and
     * should be treated like a session identifier.
     * </p>
     * <p><b>Required.</b></p>
     */
    private String flow_token;
}
