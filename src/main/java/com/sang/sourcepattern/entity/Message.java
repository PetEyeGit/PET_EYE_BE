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

    /** Loại kênh: ADMIN_SUPPORT, INTERNAL_STAFF, DIRECT */
    @Builder.Default
    String channelType = "ADMIN_SUPPORT";

    /** Email người gửi */
    String senderEmail;

    /** Email người nhận (chỉ dùng cho loại DIRECT) */
    String recipientEmail;

    /** ID của thực thể mục tiêu (ví dụ: bookingId) */
    Integer targetId;

    /** ADMIN | SHOP_OWNER | STAFF */
    String senderRole;

    @Column(columnDefinition = "TEXT")
    String content;

    /** URL of the attached file (Cloudinary, etc.) */
    String attachmentUrl;

    /** Type: IMAGE, FILE, VIDEO */
    String attachmentType;

    /** Original file name */
    String attachmentName;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    boolean isRead = false;
}
