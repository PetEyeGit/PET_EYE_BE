package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageRequest {
    int shopId;
    String channelType;
    String recipientEmail;
    Integer targetId;
    String content;
    String attachmentUrl;
    String attachmentType;
    String attachmentName;
}
