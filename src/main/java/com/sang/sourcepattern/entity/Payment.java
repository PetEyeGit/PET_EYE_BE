package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @OneToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    BigDecimal amount;
    String method; // VNPAY, MOMO, CASH, BANK_TRANSFER
    String status; // PENDING, SUCCESS, FAILED, CANCELLED
    String gatewayTransactionId; // Id từ cổng thanh toán
    
    @Builder.Default
    LocalDateTime paymentTime = LocalDateTime.now();
    
    String description;
}
