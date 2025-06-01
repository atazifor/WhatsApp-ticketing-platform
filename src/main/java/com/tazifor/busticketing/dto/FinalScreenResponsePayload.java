package com.tazifor.busticketing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response sent to WhatsApp when the flow is completed.
 *
 * This triggers a "Flow Completed" message to be sent to the user,
 * and the flow UI is closed.
 */
@Data
@NoArgsConstructor
public final class FinalScreenResponsePayload implements FlowResponsePayload {

    /**
     * Must always be "SUCCESS" to signal Flow completion.
     */
    private String screen = "SUCCESS";

    /**
     * Contains response data used in the final message.
     * Must include "flow_token", and may contain other parameters.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ExtensionMessageResponse data;

    public FinalScreenResponsePayload(ExtensionMessageResponse data) {
        this.data = data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtensionMessageResponse {
        private Map<String, Object> params;

        public void validate() {
            if (params == null || !params.containsKey("flow_token")) {
                throw new IllegalArgumentException("Missing required 'flow_token' in params");
            }
        }
    }
}
