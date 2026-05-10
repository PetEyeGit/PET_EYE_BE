package com.sang.sourcepattern.dto.response.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIHistoryItem {
    Long id;
    String role;
    String content;
    String toolResultJson;
    LocalDateTime createdAt;
}
