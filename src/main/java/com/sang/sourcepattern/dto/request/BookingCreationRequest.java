package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreationRequest {

    @NotNull(message = "SHOP_ID_REQUIRED")
    Integer shopId;

    /**
     * serviceId chính (service đầu tiên / duy nhất).
     * Vẫn giữ để tương thích ngược.
     */
    @NotNull(message = "SERVICE_ID_REQUIRED")
    Integer serviceId;

    /**
     * Danh sách tất cả service IDs khi user chọn nhiều dịch vụ.
     * Nếu null/empty → chỉ dùng serviceId.
     * Nếu có → serviceId = serviceIds[0], tổng price = sum(services).
     */
    List<Integer> serviceIds;

    @NotNull(message = "PET_ID_REQUIRED")
    Integer petId;

    /** Optional — null means no specific staff preference */
    Integer staffId;

    @NotNull(message = "APPOINTMENT_DATETIME_REQUIRED")
    @Future(message = "APPOINTMENT_MUST_BE_FUTURE")
    LocalDateTime appointmentDatetime;

    LocalDateTime checkIn;
    LocalDateTime checkOut;

    String note;
    String cageSize;
    String roomType;

    /**
     * PAYOS  → tạo PayOS payment link (default)
     * CASH   → đặt lịch ngay, không cần thanh toán online
     */
    @Builder.Default
    String paymentMethod = "PAYOS";
}
