package com.tazifor.busticketing.service.screens;

import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.ScreenHandlerResult;
import com.tazifor.busticketing.model.BookingState;

/**
 * Given a FlowDataExchangePayload and current BookingState, return
 * either the next‐screen payload or a final payload.
 */
public interface ScreenHandler {
    /**
     * @param requestPayload  the decrypted payload from WhatsApp (including "screen", "data", "flow_token")
     * @param state           the mutable BookingState so far
     * @return a ScreenHandlerResult (has newState and either NextScreenResponsePayload or FinalScreenResponsePayload)
     */
    ScreenHandlerResult handleDataExchange(FlowDataExchangePayload requestPayload, BookingState state);
}