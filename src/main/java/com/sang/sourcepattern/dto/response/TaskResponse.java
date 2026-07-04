package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskResponse {
    int bookingId;
    int shopId;
    String shopName;

    // Pet info
    int petId;
    String petName;

    // Customer info
    int customerId;
    String customerName;
    String customerEmail;
    String customerPhone;

    // Service info
    int serviceId;
    String serviceName;
    java.math.BigDecimal servicePrice;

    // Assigned staff
    Integer staffId;
    String staffName;

    LocalDateTime appointmentDatetime;

    /**
     * Task lifecycle statuses:
     * CONFIRMED → IN_PROGRESS → COMPLETED | CANCELLED
     */
    String status;

    LocalDateTime checkOut;

    LocalDateTime checkOutDatetime;
    LocalDateTime serviceStartDatetime;
    LocalDateTime serviceEndDatetime;
    String petWeight;
    String roomType;

    boolean cameraEnabled;
    String rtspLink;
    String category;


    // Multiple services (Many-to-Many)
    java.util.List<ServiceItem> services;
    java.util.List<Integer> completedServiceIds;

    String note;
    String paymentMethod; // e.g. "PAYOS" or "CASH_DEPOSIT"
    String paymentStatus; // e.g. "SUCCESS", "PENDING"
    String cancellationReason;
    String bankName;
    String bankAccount;
    String accountHolder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ServiceItem {
        int serviceId;
        String serviceName;
        java.math.BigDecimal servicePrice;
        String completedAt;
    }
}
