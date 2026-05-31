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

    LocalDateTime checkOutDatetime;
    LocalDateTime serviceStartDatetime;
    LocalDateTime serviceEndDatetime;
    String cageSize;
    String roomType;

    boolean cameraEnabled;
    String rtspLink;
    String category;

    String note;
    String cancellationReason;
    String bankName;
    String bankAccount;
    String accountHolder;
    LocalDateTime createdAt;
}
