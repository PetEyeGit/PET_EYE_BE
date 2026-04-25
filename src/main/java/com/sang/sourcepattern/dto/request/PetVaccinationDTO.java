package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetVaccinationDTO {
    String name;
    String drug;
    String clinic;
    LocalDateTime date;
    String status;
}
