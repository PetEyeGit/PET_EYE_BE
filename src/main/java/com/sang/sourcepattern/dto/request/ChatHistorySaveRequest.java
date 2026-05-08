package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatHistorySaveRequest {

    @NotBlank(message = "role is required")
    String role;           // "user" | "assistant"

    @NotBlank(message = "content is required")
    String content;

    String toolResultJson; // JSON string, tuỳ chọn
}
