package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Returned by /bookings/initiate-payment.
 * FE redirects user to checkoutUrl; no booking exists yet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitiatePaymentResponse {
    Long orderCode;
    String checkoutUrl;
    String qrCode;
    int amount;
    String description;
}
