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


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "booking_services",
        joinColumns = @JoinColumn(name = "booking_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    java.util.Set<Service> services = new java.util.HashSet<>();

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

    @Builder.Default
    Boolean isReminderSent = false;

    String note;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    Voucher appliedVoucher;

    Double discountAmount;

    String cancellationReason;
    LocalDateTime cancellationRequestedAt;
    String bankName;
    String bankAccount;
    String accountHolder;

    @Column(columnDefinition = "TEXT")
    String rtspLink;

    @Column(name = "completed_service_ids", columnDefinition = "TEXT")
    String completedServiceIds;

    @Column(name = "completed_service_times", columnDefinition = "TEXT")
    String completedServiceTimes;

    /** PayOS order code — unique long used to match webhook callback */
    Long payosOrderCode;

    String cameraRtspUrl;
    String cameraStreamUrl;
    LocalDateTime cameraConfiguredAt;

    LocalDateTime checkIn;
    LocalDateTime checkOut;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @org.hibernate.annotations.UpdateTimestamp
    LocalDateTime updatedAt;

    @Builder.Default
    String createdBy = "USER";
}
