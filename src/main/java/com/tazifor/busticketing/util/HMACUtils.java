package com.tazifor.busticketing.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class HMACUtils {
    private final static Logger logger = LoggerFactory.getLogger(HMACUtils.class);
    /**
     * Compute HMAC-SHA256(rawBody, appSecret) and compare (timing-safe) to the hex from X-Hub-Signature-256.
     */
    public static boolean isSignatureValid(String rawBody, String signatureHeader, String appSecret) {
        if (appSecret == null || appSecret.isEmpty()) {
            // If no secret is configured, skip validation but log a warning.
            logger.warn("Warning: app.secret not set; skipping request signature verification.");
            return true;
        }

        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            logger.warn("Missing or invalid X-Hub-Signature-256 header.");
            return false;
        }

        String hexFromHeader = signatureHeader.substring("sha256=".length());
        byte[] headerBytes;
        try {
            headerBytes = hexToBytes(hexFromHeader);
        } catch (IllegalArgumentException iae) {
            logger.warn("Could not parse signature hex: " + iae.getMessage());
            return false;
        }

        byte[] computedHmac;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            computedHmac = hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.warn("Error computing HMAC-SHA256: " + e.getMessage());
            return false;
        }

        // timing-safe compare
        if (computedHmac.length != headerBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < computedHmac.length; i++) {
            result |= (computedHmac[i] ^ headerBytes[i]);
        }
        return result == 0;
    }

    /**
     * Turn a hex string (lowercase or uppercase) into a byte[]. Throws IllegalArgumentException
     * if length is odd or found non-hex chars.
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex length");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Found non-hex character");
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
