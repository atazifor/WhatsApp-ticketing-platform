package com.tazifor.busticketing.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.config.properties.WhatsAppProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.CorePublisher;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * A simple client for sending messages to the WhatsApp Cloud API.
 */
@Component
@RequiredArgsConstructor
public class WhatsAppApiClient {
    private final static Logger logger = LoggerFactory.getLogger(WhatsAppApiClient.class);

    @Getter
    private final WebClient webClient;

    @Autowired
    public WhatsAppApiClient(WhatsAppProperties config) {
        // Preconfigure WebClient with base URL and Authorization header
        //"https://graph.facebook.com/v22.0/" + phoneNumberId + "/messages"
        String uri = String.format("%s/%s/%s/messages", config.baseUrl(), config.version(), config.phoneNumberId());
        this.webClient = WebClient.builder()
            .baseUrl(uri)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.accessToken())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }




    public Mono<Void> sendText(String to, String text) {
        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", to,
            "type", "text",
            "text", Map.of("body", text)
        );

        return sendRawMessage(body);
    }


    public Mono<Void> sendTemplateMessage(String templateName, String languageCode, String recipientPhone, List<Map<String, Object>> components) {
        Map<String, Object> template = Map.of(
            "name", templateName,
            "language", Map.of("code", languageCode),
            "components", components
        );

        Map<String, Object> requestBody = Map.of(
            "messaging_product", "whatsapp",
            "to", recipientPhone,
            "type", "template",
            "template", template
        );

        return getWebClient()
            .post()
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .doOnNext(response -> logger.info("✅ Template sent: {}", response))
            .doOnError(error -> logger.info("❌ Error sending message: {}", error.getMessage()))
            .then(Mono.empty());
    }

    /**
     * Sends a “raw” JSON raw payload directly to the WhatsApp /messages endpoint.
     *
     * @param payload a Map representing exactly what the Cloud API expects:
     *                {
     *                  "messaging_product": "whatsapp",
     *                  "to": "<USER_NUMBER>",
     *                  "type": "interactive",
     *                  "interactive": { … }
     *                }
     */
    public Mono<Void> sendRawMessage(Map<String, Object> payload) {
        return webClient
            .post()
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class);
    }

    // ---------------------------------------------
    // 3) Send an interactive “Flow” message
    // This tells WhatsApp to open a Flow UI on the user’s phone.
    // ---------------------------------------------
    public Mono<Void> sendFlowMessage(String to,
                                      String flowId,
                                      Map<String, Object> flowActionPayload) {
        // flowActionPayload may be empty or contain “data” for the first screen
        Map<String, Object> interactive = Map.<String, Object>of(
            "type", "flow",
            "body", Map.of("text", "Let me guide you to book your ticket!"), // optional prompt text
            "action", Map.of(
                "name", "flow",
                "parameters", Map.of(
                    "flow_message_version", "3",
                    "flow_cta", "Begin!",
                    "flow_id", flowId,
                    "flow_token", flowActionPayload.getOrDefault("flow_token", generateRandomToken()),
                    "flow_action", "data_exchange" //get data from endpoint url
                )
            )
        );

        // If you want to pass initial data, tack it onto parameters:
        if (flowActionPayload.containsKey("data")) {
            interactive = Map.of(
                "type", "flow",
                "body", Map.of("text", "Let me guide you to book your ticket!"),
                "action", Map.of(
                    "name", "flow",
                    "parameters", Map.<String, Object>of(
                        "flow_message_version", "3",
                        "flow_cta", "Begin!",
                        "flow_id", flowId,
                        "flow_token", flowActionPayload.getOrDefault("flow_token", generateRandomToken()),
                        "flow_action_payload", Map.of("data", flowActionPayload.get("data"))
                    )
                )
            );
        }

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", to,
            "type", "interactive",
            "interactive", interactive
        );

        return webClient
            .post()
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Void.class);
    }

    // Helper to generate a random flow token if you need one
    private String generateRandomToken() {
        return java.util.UUID.randomUUID().toString();
    }
}
