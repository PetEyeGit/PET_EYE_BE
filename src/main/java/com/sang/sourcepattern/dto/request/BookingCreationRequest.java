package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreationRequest {
    int userId;
    int shopId;
    int serviceId;
    int petId;
    int staffId;
    LocalDateTime appointmentDatetime;
    String note;
}
