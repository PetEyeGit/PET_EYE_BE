package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    /** ID của shop liên quan đến cuộc hội thoại */
    int shopId;

    /** Email người gửi */
    String senderEmail;

    /** ADMIN | SHOP_OWNER */
    String senderRole;

    @Column(columnDefinition = "TEXT")
    String content;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    boolean isRead = false;
}
