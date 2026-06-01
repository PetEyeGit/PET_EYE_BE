package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Step 1: FE sends this to get a PayOS checkout URL.
 * No booking is created yet — only after payment succeeds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitiatePaymentRequest {

    @NotNull(message = "SHOP_ID_REQUIRED")
    Integer shopId;

    /** serviceId chính — vẫn giữ để tương thích ngược */
    @NotNull(message = "SERVICE_ID_REQUIRED")
    Integer serviceId;

    /**
     * Danh sách tất cả service IDs khi user chọn nhiều dịch vụ.
     * Nếu null/empty → chỉ dùng serviceId.
     */
    List<Integer> serviceIds;

    @NotNull(message = "PET_ID_REQUIRED")
    Integer petId;
    Integer staffId;

    @NotNull(message = "APPOINTMENT_DATETIME_REQUIRED")
    @Future(message = "APPOINTMENT_MUST_BE_FUTURE")
    LocalDateTime appointmentDatetime;

    LocalDateTime checkIn;
    LocalDateTime checkOut;

    String note;
    String cageSize;
    String roomType;

    Integer userVoucherId;
}
