package com.tazifor.busticketing.dto.crypto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlowEncryptedPayload {
    private String encryptedFlowData; //The encrypted request payload.
    private String encryptedAesKey; //The encrypted 128-bit AES key.
    private String initialVector; //The 128-bit initialization vector.

}
