package com.tazifor.busticketing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.service.EncryptionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class EncryptionTest {
    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEncryptDecryptBookingState() throws Exception {
        // Given
        BookingState original = new BookingState("destination", "new_york");
        String json = objectMapper.writeValueAsString(original);

        // Generate random AES key and IV
        byte[] aesKey = FlowCryptoUtils.generateAesKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        // Encrypt AES key with WhatsApp's public key
        PublicKey publicKey = encryptionService.getPublicKey();
        byte[] encryptedAesKey = FlowCryptoUtils.encryptAesKey(aesKey, publicKey);

        // Encrypt payload using AES
        byte[] encryptedPayload = FlowCryptoUtils.encryptJson(json, aesKey, iv);

        // Simulate request values (Base64-encoded)
        String encryptedPayloadB64 = Base64.getEncoder().encodeToString(encryptedPayload);
        String encryptedAesKeyB64 = Base64.getEncoder().encodeToString(encryptedAesKey);
        String ivB64 = Base64.getEncoder().encodeToString(iv);

        // ----------------------
        // Now simulate decrypting it like WhatsApp does
        // ----------------------
        byte[] decodedPayload = Base64.getDecoder().decode(encryptedPayloadB64);
        byte[] decodedAesKey = FlowCryptoUtils.decryptAesKey(Base64.getDecoder().decode(encryptedAesKeyB64), encryptionService.getPrivateKey());
        byte[] decodedIv = Base64.getDecoder().decode(ivB64);
        String decryptedJson = FlowCryptoUtils.decryptJson(decodedPayload, decodedAesKey, decodedIv);

        BookingState decryptedState = objectMapper.readValue(decryptedJson, BookingState.class);

        // Then
        assertEquals(original.getStep(), decryptedState.getStep());
        assertEquals(original.getDestination(), decryptedState.getDestination());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class BookingState {
        private String step;
        private String destination;
    }

}