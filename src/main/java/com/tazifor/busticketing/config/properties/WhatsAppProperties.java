package com.tazifor.busticketing.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp.api")
public record WhatsAppProperties(
        String baseUrl,
        String version,
        String accessToken,
        String whatsappBusinessAccountId,
        String phoneNumber,
        String phoneNumberId
) {}
