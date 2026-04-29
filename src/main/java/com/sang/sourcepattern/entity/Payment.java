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

    /** PAYOS */
    @Builder.Default
    String method = "PAYOS";

    /** PENDING, SUCCESS, FAILED, CANCELLED */
    @Builder.Default
    String status = "PENDING";

    /** PayOS order code (same as Booking.payosOrderCode) */
    Long payosOrderCode;

    /** QR / checkout URL returned by PayOS */
    @Column(length = 1000)
    String checkoutUrl;

    /** Transaction ID returned by PayOS webhook */
    String gatewayTransactionId;

    @Builder.Default
    LocalDateTime paymentTime = LocalDateTime.now();

    String description;
}
