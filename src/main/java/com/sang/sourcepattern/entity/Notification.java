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
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    String title;
    String content;

    /** UUID dùng để group các thông báo cùng 1 đợt gửi */
    String broadcastId;

    /**
     * Loại thông báo:
     * GENERAL, PROMOTION, REMINDER, SYSTEM, BOOKING
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    NotificationType notificationType = NotificationType.GENERAL;

    public enum NotificationType {
        GENERAL, PROMOTION, REMINDER, SYSTEM, BOOKING
    }

    @Builder.Default
    boolean isRead = false;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}
