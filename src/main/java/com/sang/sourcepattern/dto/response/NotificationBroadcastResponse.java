package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationBroadcastResponse {
    String broadcastId;
    String title;
    String content;
    String targetType;
    String notificationType;
    long totalSent;
    long totalRead;
    LocalDateTime createdAt;
}
