package com.tazifor.busticketing.dto.crypto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlowEncryptedPayload {
    @JsonProperty("encrypted_flow_data")
    private String encryptedFlowData; //The encrypted request payload.
    @JsonProperty("encrypted_aes_key")
    private String encryptedAesKey; //The encrypted 128-bit AES key.
    @JsonProperty("initial_vector")
    private String initialVector; //The 128-bit initialization vector.

}
