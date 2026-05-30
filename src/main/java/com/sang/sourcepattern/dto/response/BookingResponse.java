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

    // Payment info
    String checkoutUrl;
    String paymentStatus;
    String paymentMethod;

    String cameraRtspUrl;
    String cameraStreamUrl;
    boolean cameraEnabled;
    LocalDateTime cameraConfiguredAt;
    LocalDateTime checkIn;
    LocalDateTime checkOut;
    LocalDateTime serviceEndDatetime;

    // Service Boarding fields
    String cageSize;
    String roomType;
}
