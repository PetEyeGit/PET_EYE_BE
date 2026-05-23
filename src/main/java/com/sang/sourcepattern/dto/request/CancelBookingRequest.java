package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelBookingRequest {
    @NotBlank
    String reason;

    @NotBlank
    String bankName;

    @NotBlank
    String bankAccount;

    @NotBlank
    String accountHolder;
}
