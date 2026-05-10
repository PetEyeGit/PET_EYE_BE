package com.sang.sourcepattern.dto.response.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIChatResponse {

    /** Text response from AI */
    String text;

    /**
     * JSON string of ToolResult (shop_list, booking_picker, pet_list, etc.)
     * Only present for USER_CHAT agent when a tool was executed.
     */
    String toolResultJson;

    /** Session ID for this conversation */
    String sessionId;

    /** Which Gemini model was actually used */
    String model;
}
