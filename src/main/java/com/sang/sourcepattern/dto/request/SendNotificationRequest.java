package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendNotificationRequest {

    @NotBlank(message = "Title is required")
    String title;

    @NotBlank(message = "Content is required")
    String content;

    /**
     * Kiểu gửi:
     * - SINGLE     : gửi cho 1 user cụ thể (cần userId)
     * - ALL_USERS  : gửi cho tất cả user có role USER
     * - ALL_SHOPS  : gửi cho tất cả user có role SHOP_OWNER
     * - ALL        : gửi cho toàn bộ người dùng
     */
    @NotNull(message = "Target type is required")
    TargetType targetType;

    /** Chỉ cần khi targetType = SINGLE */
    Integer userId;

    /** Loại thông báo, mặc định là GENERAL nếu không truyền */
    @Builder.Default
    NotificationType notificationType = NotificationType.GENERAL;

    public enum TargetType {
        SINGLE, ALL_USERS, ALL_SHOPS, ALL
    }

    public enum NotificationType {
        GENERAL, PROMOTION, REMINDER, SYSTEM, BOOKING
    }
}
