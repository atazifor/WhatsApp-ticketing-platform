package com.tazifor.busticketing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * When the flow is complete, WhatsApp expects {"screen":"SUCCESS","data":{ … }}.
 * We build one of these, serialize to Map<String,Object>, then encrypt that JSON.
 *
 * Plain text (before encryption) example:
 * {
 *   "screen": "SUCCESS",
 *   "data": {
 *     "extension_message_response": {
 *       "params": {
 *         "flow_token": "<UUID>"
 *       }
 *     }
 *   }
 * }
 */
@Data
@NoArgsConstructor
public final class FinalScreenResponsePayload implements FlowResponsePayload {
    private final static Logger logger = LoggerFactory.getLogger(FinalScreenResponsePayload.class);
    /**
     * Must always be "SUCCESS" to signal Flow completion.
     */
    private String screen = "SUCCESS";

    /**
     * Contains response data used in the final message.
     * Must include "flow_token", and may contain other parameters.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> data;

    public FinalScreenResponsePayload(ExtensionMessageResponse data) {
        this.data = Map.of("extension_message_response", data);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtensionMessageResponse {
        private Map<String, Object> params;

        public void validate() {
            if (params == null || !params.containsKey("flow_token")) {
                logger.warn("Missing required 'flow_token' in params");
                throw new IllegalArgumentException("Missing required 'flow_token' in params");
            }
        }
    }
}
