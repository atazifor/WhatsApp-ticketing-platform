package com.nourri.busticketing.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp.encryption")
public record EncryptionConfig(
        String privateKeyPath,
        String publicKeyPath
) {}
