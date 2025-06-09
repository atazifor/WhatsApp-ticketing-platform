package com.tazifor.busticketing.service;

import com.tazifor.busticketing.config.properties.EncryptionConfig;
import com.tazifor.busticketing.util.EncryptionUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

/**
 * Loads RSA keypair from PEM files and provides methods to:
 * - decrypt incoming FlowEncryptedInput payloads
 * - encrypt outgoing state JSON blobs for WhatsApp Flows
 */
@Service
@RequiredArgsConstructor
public class FlowEncryptionService {
    private final static Logger logger = LoggerFactory.getLogger(FlowEncryptionService.class);
    @Getter private PrivateKey privateKey;
    @Getter private PublicKey publicKey;

    private final EncryptionConfig config;

    @PostConstruct
    public void loadKeys() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        this.privateKey = loadPEMPrivateKey(config.privateKeyPath());
        this.publicKey  = loadPEMPublicKey(config.publicKeyPath());
    }

    private PrivateKey loadPEMPrivateKey(String path) throws Exception {
        try (PEMParser parser = new PEMParser(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            PrivateKeyInfo keyInfo = PrivateKeyInfo.getInstance(parser.readObject());
            return new JcaPEMKeyConverter().getPrivateKey(keyInfo);
        }
    }

    private PublicKey loadPEMPublicKey(String path) throws Exception {
        try (PEMParser parser = new PEMParser(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            SubjectPublicKeyInfo keyInfo = (SubjectPublicKeyInfo) parser.readObject();
            return new JcaPEMKeyConverter().getPublicKey(keyInfo);
        }
    }

    /**
     * Decrypts the incoming FlowEncryptedInput parts into clear JSON state and returns
     * the raw JSON, AES key, and IV for further processing.
     */
    public DecryptionResult decryptPayload(String encryptedFlowDataB64,
                                           String encryptedAesKeyB64,
                                           String ivB64) throws Exception {
        byte[] encryptedFlowData = Base64.getDecoder().decode(encryptedFlowDataB64);
        byte[] encryptedAesKey   = Base64.getDecoder().decode(encryptedAesKeyB64);
        byte[] iv                = Base64.getDecoder().decode(ivB64);

        // 1. decrypt AES key with RSA private key
        byte[] aesKey = EncryptionUtils.decryptAesKey(encryptedAesKey, privateKey);

        // 2. decrypt JSON payload with AES/GCM
        String clearJson = EncryptionUtils.decryptJson(encryptedFlowData, aesKey, iv);

        return new DecryptionResult(clearJson, aesKey, iv);
    }

    /**
     * Encrypts a clear JSON string representing the new state using the provided AES key and IV.
     * Returns the Base64-encoded ciphertext.
     */
    public String encryptPayload(String clearJson, byte[] aesKey, byte[] iv) throws Exception {
        // WhatsApp expects encryption with a flipped IV
        byte[] flippedIv = EncryptionUtils.flipIv(iv);
        byte[] cipherBytes = EncryptionUtils.encryptJson(clearJson, aesKey, flippedIv);
        return Base64.getEncoder().encodeToString(cipherBytes);
    }

    /**
         * Container for the results of decryption:
         * - the clear JSON payload
         * - the AES key for subsequent encryption
         * - the original IV
         */
        public record DecryptionResult(String clearJson, byte[] aesKey, byte[] iv) {
    }
}
