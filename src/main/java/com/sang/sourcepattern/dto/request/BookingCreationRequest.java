package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreationRequest {

    @NotNull(message = "SHOP_ID_REQUIRED")
    Integer shopId;

    @NotNull(message = "SERVICE_ID_REQUIRED")
    Integer serviceId;

    @NotNull(message = "PET_ID_REQUIRED")
    Integer petId;

    /** Optional — null means no specific staff preference */
    Integer staffId;

    @NotNull(message = "APPOINTMENT_DATETIME_REQUIRED")
    @Future(message = "APPOINTMENT_MUST_BE_FUTURE")
    LocalDateTime appointmentDatetime;

    String note;

    /**
     * PAYOS  → tạo PayOS payment link (default)
     * CASH   → đặt lịch ngay, không cần thanh toán online
     */
    @Builder.Default
    String paymentMethod = "PAYOS";
}
