package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetVaccinationResponse {
    Integer id;
    Integer petId;
    Integer bookingId;
    Integer staffId;
    String staffName;
    String shopName;
    String name;
    String drug;
    String clinic;
    LocalDateTime date;
    String status;
}
