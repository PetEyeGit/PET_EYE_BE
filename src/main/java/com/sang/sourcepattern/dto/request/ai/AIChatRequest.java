package com.sang.sourcepattern.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIChatRequest {

    /** USER_CHAT | SHOP_ASSISTANT | ADMIN_ASSISTANT */
    @NotBlank(message = "agentType is required")
    String agentType;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "Message too long (max 2000 chars)")
    String message;

    /** Optional — client can pass a sessionId for multi-session support */
    String sessionId;
}
