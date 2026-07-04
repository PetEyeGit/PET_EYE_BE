package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopCreateBookingRequest {

    @NotNull(message = "CUSTOMER_ID_REQUIRED")
    Integer customerId;

    @NotNull(message = "SHOP_ID_REQUIRED")
    Integer shopId;

    @NotNull(message = "SERVICE_IDS_REQUIRED")
    List<Integer> serviceIds;

    @NotNull(message = "PET_ID_REQUIRED")
    Integer petId;

    Integer staffId;

    @NotNull(message = "APPOINTMENT_DATETIME_REQUIRED")
    @Future(message = "APPOINTMENT_MUST_BE_FUTURE")
    LocalDateTime appointmentDatetime;

    LocalDateTime checkIn;
    LocalDateTime checkOut;

    String note;

    String petWeight;
    String roomType;
}
