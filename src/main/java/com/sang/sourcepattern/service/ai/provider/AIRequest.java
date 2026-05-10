package com.sang.sourcepattern.service.ai.provider;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIRequest {

    String systemPrompt;

    /** Conversation history: [{role, content}, ...] */
    List<Map<String, String>> messages;

    /**
     * Gemini function_declarations schemas.
     * Empty list = no function calling (Shop/Admin AI).
     */
    @Builder.Default
    List<Map<String, Object>> tools = List.of();

    /** Preferred provider name, defaults to "gemini" */
    @Builder.Default
    String preferredProvider = "gemini";

    /**
     * IMPORTANT: Must use @Builder.Default so Lombok Builder
     * doesn't reset these to Java primitives defaults (0 / 0.0).
     * Gemini rejects maxOutputTokens = 0 with HTTP 400.
     */
    @Builder.Default
    double temperature = 0.6;

    @Builder.Default
    int maxOutputTokens = 2048;
}
