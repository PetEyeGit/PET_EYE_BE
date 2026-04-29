package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

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
    @NotNull Integer serviceId;
    @NotNull Integer petId;
    Integer staffId;

    @NotNull @Future
    LocalDateTime appointmentDatetime;

    String note;
}
