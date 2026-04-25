package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetMedicalRecordDTO {
    String diagnosis;
    String treatment;
    String prescription;
    LocalDateTime visitDate;
    String veterinarianNote;
}
