package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationResponse {
    int id; // This is the shopId (or 0 for admin support)
    String shopName;
    String logoUrl;
    String lastMessage;
    LocalDateTime lastMessageAt;
    int unreadCount;
}
