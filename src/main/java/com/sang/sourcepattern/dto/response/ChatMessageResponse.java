package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {
    int id;
    int shopId;
    String channelType;
    String senderEmail;
    String recipientEmail;
    Integer targetId;
    String senderRole;
    String content;
    String attachmentUrl;
    String attachmentType;
    String attachmentName;
    LocalDateTime createdAt;
    boolean isRead;
}
