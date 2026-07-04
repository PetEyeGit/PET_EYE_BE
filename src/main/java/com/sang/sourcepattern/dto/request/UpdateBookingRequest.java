package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateBookingRequest {

    @NotNull(message = "PET_ID_REQUIRED")
    Integer petId;

    Integer staffId;

    @NotNull(message = "SERVICE_IDS_REQUIRED")
    List<Integer> serviceIds;

    String petWeight;
    String roomType;

    String note;
}
