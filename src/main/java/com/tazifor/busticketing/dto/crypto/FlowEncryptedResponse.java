package com.tazifor.busticketing.dto.crypto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FlowEncryptedResponse {
    private String encryptedData;
    private String iv;
}
