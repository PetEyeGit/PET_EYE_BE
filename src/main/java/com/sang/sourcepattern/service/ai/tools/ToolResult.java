package com.sang.sourcepattern.service.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ToolResult {

    /**
     * Type of result — matches frontend ToolResult union type:
     * shop_list | shop_detail | pet_list | pet_detail |
     * booking_picker | booking_success | error
     */
    String type;

    /** Full data payload — serialized to JSON for frontend */
    Object data;

    /**
     * Compact summary sent back to Gemini as functionResponse.
     * Smaller than data to save tokens.
     */
    Map<String, Object> geminiSummary;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ToolResult error(String message) {
        return ToolResult.builder()
                .type("error")
                .data(Map.of("message", message))
                .geminiSummary(Map.of("error", message))
                .build();
    }

    /** Serialize full data to JSON string for storage / frontend */
    public String toJson() {
        try {
            Map<String, Object> wrapper = Map.of("type", type, "data", data != null ? data : Map.of());
            return MAPPER.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"error\",\"data\":{\"message\":\"Serialization failed\"}}";
        }
    }
}
