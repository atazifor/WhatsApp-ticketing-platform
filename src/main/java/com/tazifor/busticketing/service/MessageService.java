package com.tazifor.busticketing.service;

import com.tazifor.busticketing.client.WhatsAppApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final String FLOW_ID = "1887742978711236";
    private final WhatsAppApiClient apiClient;
    private final TemplateService templateService;
    Logger logger = LoggerFactory.getLogger(MessageService.class);

    /**
     * Processes a plain text message from a user:
     *  - If a TemplatePayload is provided by TemplateService, send that template.
     *  - Otherwise, send a fallback text reply.
     */
    public void processIncomingText(String from, String messageText) {
        templateService.chooseTemplate(messageText).ifPresentOrElse(
            template -> {
                // Send the chosen template
                logger.info("Sending template '{}' to {}", template.name(), from);
                apiClient.sendTemplateMessage(
                    template.name(),
                    template.languageCode(),
                    from,
                    template.components()
                ).subscribe();
            },
            () -> {
                // No template chosen. Check for "Book Ticket" or fallback.
                if (messageText.contains("book")) {
                    logger.info("Initiating Ticketing Flow for user {}", from);
                    apiClient.sendFlowMessage(
                        from,
                        FLOW_ID,
                        Map.of()   // optional initial flow_action_payload.data
                    ).subscribe();
                }else {
                    logger.info("Sending fallback text to {}", from);
                    apiClient.sendText(
                        from,
                        "Sorry, I didn’t understand. Type 'Book Ticket' to get started."
                    ).subscribe();
                }
            }
        );
    }

    /**
     * Sends a raw Flow response (interactive payload) that was constructed by FlowService.
     *
     * @param to           The recipient’s phone number (in international format, no “+”).
     * @param responseBody A Map that already contains all required keys:
     *                     {
     *                       "messaging_product": "whatsapp",
     *                       "to": "<USER_NUMBER>",
     *                       "type": "interactive",
     *                       "interactive": { … }
     *                     }
     */
    public Mono<Void> sendRawFlowMessage(String to, Map<String, Object> responseBody) {
        // Ensure the "to" field is present (some callers include it, but double‐check)
        responseBody.put("to", to);
        return apiClient.sendRawMessage(responseBody);
    }


}
