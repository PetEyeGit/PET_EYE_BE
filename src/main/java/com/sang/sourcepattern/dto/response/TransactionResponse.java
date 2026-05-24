package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionResponse {
    int id;
    String type;
    BigDecimal amount;
    String paymentMethod;
    String status;
    Long payosOrderCode;
    String gatewayTransactionId;
    String description;
    LocalDateTime createdAt;
    
    // Booking details (if applicable)
    Integer bookingId;
    String shopName;
    String serviceName;
}
