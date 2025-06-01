package com.tazifor.busticketing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates logic for choosing which WhatsApp template to send
 * based on user‐provided text (or other criteria).
 */
@Service
@RequiredArgsConstructor
public class TemplateService {
    /**
     * If the user’s message text matches a known template trigger,
     * return a TemplatePayload. Otherwise return empty.
     */
    public Optional<TemplatePayload> chooseTemplate(String messageText) {
        if (messageText.toLowerCase().contains("reminder")) {
            return Optional.of(new TemplatePayload(
                "event_reminder",               // template name
                "en_US",                        // language code
                List.of(
                    Map.<String, Object>of(
                        "type", "body",
                        "parameters", List.of(
                            Map.of("type", "text", "text", "Startup Pitch Night"),
                            Map.of("type", "text", "text", "Nourri Express"),
                            Map.of("type", "text", "text", "May 5"),
                            Map.of("type", "text", "text", "5:00 PM"),
                            Map.of("type", "text", "text", "Djeuga Palace")
                        )
                    )
                )
            ));
        } else if (messageText.toLowerCase().contains("apt") ||
            messageText.toLowerCase().contains("appointment")) {
            return Optional.of(new TemplatePayload(
                "name_dob_capture",
                "en",
                List.of(
                    Map.of(
                        "type", "header",
                        "parameters", List.of(Map.of(
                            "type", "image",
                            "image", Map.of("link", "https://i.imgur.com/cRnpp1Q.jpeg")
                        ))
                    ),
                    Map.of(
                        "type", "button",
                        "sub_type", "flow",
                        "index", "0",
                        "parameters", List.of(Map.of(
                            "type", "action",
                            "action", Map.of(
                                "flow_token", "TEST_TOKEN",
                                "flow_action_data", Map.of()
                            )
                        ))
                    )
                )
            ));
        } else {
            return Optional.empty();
        }
    }

    /**
     * A simple container for data needed to send a template:
     * - the template name
     * - the language code
     * - the optional list of components (header/body/button slots)
     */
    public record TemplatePayload(
        String name,
        String languageCode,
        List<Map<String, Object>> components
    ) {
    }
}
