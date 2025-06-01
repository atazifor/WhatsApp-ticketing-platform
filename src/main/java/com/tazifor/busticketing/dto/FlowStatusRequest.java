package com.tazifor.busticketing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlowStatusRequest {
    private String flow_token;
    private String status;
    private long timestamp;
    private String data; // Encrypted data
}
