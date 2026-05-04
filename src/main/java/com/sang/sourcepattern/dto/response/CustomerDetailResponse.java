package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerDetailResponse {
    CustomerItemResponse customerInfo;
    List<PetResponse> pets;
    List<BookingResponse> bookingHistory;
}
