package com.tazifor.busticketing.dto;

import com.tazifor.busticketing.model.BookingState;

public record ScreenHandlerResult(BookingState newState, FlowResponsePayload response) {}

