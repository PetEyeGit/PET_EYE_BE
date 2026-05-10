package com.sang.sourcepattern.service.ai.provider;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIResponse {

    /** Text response — present when no function call */
    String text;

    /** Function call requested by the model */
    FunctionCall functionCall;

    /** Which model was actually used */
    String modelUsed;

    int promptTokens;
    int outputTokens;

    public boolean hasFunctionCall() {
        return functionCall != null;
    }

    public int getTotalTokens() {
        return promptTokens + outputTokens;
    }
}
