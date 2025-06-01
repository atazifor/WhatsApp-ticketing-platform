package com.tazifor.busticketing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Maps incoming requests from WhatsApp API during the flow.
 * */
public class FlowRequest {
    private String version;
    private String flow_token;
    private String state; // INIT or PENDING

    // User's form response (e.g. "destination": "new_york")
    private FlowResponse response;
}
