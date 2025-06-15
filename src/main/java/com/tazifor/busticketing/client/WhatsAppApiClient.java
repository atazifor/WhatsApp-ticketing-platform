package com.tazifor.busticketing.client;

import com.tazifor.busticketing.config.properties.WhatsAppProperties;
import com.tazifor.busticketing.whatsapp.session.SessionContextStore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
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

    private final SessionContextStore sessionContextStore;

    @Autowired
    public WhatsAppApiClient(WhatsAppProperties config, SessionContextStore sessionContextStore) {
        this.sessionContextStore = sessionContextStore;
        // Preconfigure WebClient with base URL and Authorization header
        //"https://graph.facebook.com/v22.0/" + phoneNumberId "
        String uri = String.format("%s/%s/%s", config.baseUrl(), config.version(), config.phoneNumberId());
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
            .uri("/messages")
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
            .uri("/messages")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class);
    }

    // ---------------------------------------------
    // 3) Send an interactive “Flow” message
    // This tells WhatsApp to open a Flow UI on the user’s phone.
    // ---------------------------------------------
    public Mono<Void> sendFlowMessage(String messageBody,
                                        String flowCta, //flow call to action
                                        String to,
                                      String flowId,
                                      Map<String, Object> flowActionPayload) {
        // flowActionPayload may be empty or contain “data” for the first screen

        String flowToken = (String)flowActionPayload.getOrDefault("flow_token", generateRandomToken());
        sessionContextStore.saveUser(flowToken, to);

        Map<String, Object> interactive = Map.<String, Object>of(
            "type", "flow",
            "body", Map.of("text", messageBody), // optional prompt text
            "action", Map.of(
                "name", "flow",
                "parameters", Map.of(
                    "flow_message_version", "3",
                    "flow_cta", flowCta,
                    "flow_id", flowId,
                    "flow_token", flowToken,
                    "flow_action", "data_exchange" //get data from endpoint url
                )
            )
        );

        // If you want to pass initial data, tack it onto parameters:
        if (flowActionPayload.containsKey("data")) {
            interactive = Map.of(
                "type", "flow",
                "body", Map.of("text", messageBody),
                "action", Map.of(
                    "name", "flow",
                    "parameters", Map.<String, Object>of(
                        "flow_message_version", "3",
                        "flow_cta", flowCta,
                        "flow_id", flowId,
                        "flow_token", flowToken,
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
            .uri("/messages")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Void.class);
    }

    /**F
     * 1) Decode the Base64 data URI, write to a temp PNG file.
     * 2) Build a MultiValueMap with keys "messaging_product", "type", and "file".
     * 3) POST multipart/form‐data to /media.
     * 4) Delete the temp file.
     * 5) Return the media_id.
     */
    public Mono<String> uploadMedia(String base64DataUri) {
        return Mono.fromCallable(() -> {
                if (base64DataUri == null) {
                    throw new IllegalArgumentException("Base64 data URI is null");
                }

                // Strip "data:image/...;base64," prefix if present
                String rawBase64;
                if (base64DataUri.startsWith("data:image/")) {
                    int commaIndex = base64DataUri.indexOf(',');
                    if (commaIndex < 0) {
                        throw new IllegalArgumentException("Invalid data URI (no comma): " + base64DataUri);
                    }
                    rawBase64 = base64DataUri.substring(commaIndex + 1);
                } else {
                    rawBase64 = base64DataUri;
                }

                rawBase64 = rawBase64.trim().replaceAll("\\s+", "");

                // Decode Base64 into PNG bytes
                byte[] pngBytes = java.util.Base64.getDecoder().decode(rawBase64);

                // Write bytes to a temp file
                File tempFile = File.createTempFile("wa_upload_", ".png");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(pngBytes);
                }
                return tempFile;
            })
            .flatMap(tempFile -> {
                // Build multipart/form-data map
                MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
                parts.add("messaging_product", "whatsapp");
                parts.add("type", "image/png");
                // FileSystemResource will handle Content-Disposition and content-type
                parts.add("file", new FileSystemResource(tempFile));

                return webClient
                    .post()
                    .uri("/media")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(parts))
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class).flatMap(bodyStr -> {
                            // Log full error body from WhatsApp
                            System.err.printf("Media upload failed: %s → %s%n", response.statusCode(), bodyStr);
                            return Mono.error(new RuntimeException("Media upload error: " + bodyStr));
                        })
                    )
                    .bodyToMono(Map.class)
                    .flatMap(json -> {
                        Object idObj = json.get("id");
                        if (idObj instanceof String) {
                            String mediaId = (String) idObj;
                            return Mono.just(mediaId);
                        } else {
                            return Mono.error(new RuntimeException("Unexpected /media response: " + json));
                        }
                    })
                    .doFinally(signal -> {
                        try {
                            Files.deleteIfExists(tempFile.toPath());
                        } catch (Exception ex) {
                            System.err.println("Could not delete temp file: " + tempFile);
                        }
                    });
            });
    }

    /**
     * 2) Once uploadMediaFromBase64(...) yields a media_id, call /messages to send the image.
     */
    public Mono<Void> uploadAndSendBase64Image(String to, String base64DataUri, String caption) {
        return uploadMedia(base64DataUri)
            .flatMap(mediaId -> {
                Map<String, Object> imageObj = Map.of(
                    "id", mediaId,
                    "caption", caption
                );
                Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "to", to,
                    "type", "image",
                    "image", imageObj
                );
                return webClient
                    .post()
                    .uri("/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class);
            });
    }

    // Helper to generate a random flow token if you need one
    private String generateRandomToken() {
        return java.util.UUID.randomUUID().toString();
    }
}
