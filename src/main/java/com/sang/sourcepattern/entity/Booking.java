package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    Shop shop;

    @ManyToOne
    @JoinColumn(name = "service_id")
    Service service;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    Pet pet;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    Staff staff;

    LocalDateTime appointmentDatetime;
    LocalDateTime checkOutDatetime;
    LocalDateTime serviceStartDatetime;
    LocalDateTime serviceEndDatetime;

    String cageSize;
    String roomType;

    /** PENDING_PAYMENT → CONFIRMED → COMPLETED | CANCELLED */
    @Builder.Default
    String status = "PENDING_PAYMENT";

    String note;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    Voucher appliedVoucher;

    Double discountAmount;

    String cancellationReason;
    String bankName;
    String bankAccount;
    String accountHolder;

    @Column(columnDefinition = "TEXT")
    String rtspLink;

    /** PayOS order code — unique long used to match webhook callback */
    Long payosOrderCode;

    String cameraRtspUrl;
    String cameraStreamUrl;
    LocalDateTime cameraConfiguredAt;

    LocalDateTime checkIn;
    LocalDateTime checkOut;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

}
