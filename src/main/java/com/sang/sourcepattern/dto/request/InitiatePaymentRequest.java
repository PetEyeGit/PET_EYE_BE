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

    @NotNull Integer shopId;

    /** serviceId chính — vẫn giữ để tương thích ngược */
    @NotNull Integer serviceId;

    /**
     * Danh sách tất cả service IDs khi user chọn nhiều dịch vụ.
     * Nếu null/empty → chỉ dùng serviceId.
     */
    List<Integer> serviceIds;

    @NotNull Integer petId;
    Integer staffId;

    @NotNull @Future
    LocalDateTime appointmentDatetime;

    String note;
}
