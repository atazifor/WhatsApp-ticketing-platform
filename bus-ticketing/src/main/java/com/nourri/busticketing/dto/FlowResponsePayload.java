package com.nourri.busticketing.dto;

/**
 * Base type for all valid responses to a WhatsApp data exchange request.
 * Only the permitted types represent allowed responses.
 */
public sealed interface FlowResponsePayload permits NextScreenResponsePayload, FinalScreenResponsePayload {
    String getScreen();
}
