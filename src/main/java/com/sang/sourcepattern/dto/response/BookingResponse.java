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
public class BookingResponse {
    int id;
    int userId;
    int shopId;
    String shopName;
    String shopAddress;
    int serviceId;
    String serviceName;
    BigDecimal servicePrice;
    int petId;
    String petName;
    String customerName;
    String customerEmail;
    String customerPhone;
    String customerAddress;
    Integer staffId;
    String staffName;
    LocalDateTime appointmentDatetime;
    String status;
    String note;
    String cancellationReason;
    String bankName;
    String bankAccount;
    String accountHolder;
    Long payosOrderCode;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime serviceStartDatetime;
    LocalDateTime serviceEndDatetime;

    // Payment info
    String checkoutUrl;
    String paymentStatus;
    String paymentMethod;
    BigDecimal paidAmount;
    BigDecimal discountAmount;
    BigDecimal totalAmount;
    BigDecimal remainingAmount;

    String cameraRtspUrl;
    String cameraStreamUrl;
    boolean cameraEnabled;
    LocalDateTime cameraConfiguredAt;
    LocalDateTime checkIn;
    LocalDateTime checkOut;

    // Service Boarding fields
    String petWeight;
    String roomType;

    // Category from primary service (BOARDING, CLINIC, GROOMING, etc.)
    String category;

    // Multi-service fields
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingServiceDto {
        int serviceId;
        String serviceName;
        BigDecimal servicePrice;
        String category;
        int durationMinutes;
    }
    java.util.List<BookingServiceDto> services;
}
