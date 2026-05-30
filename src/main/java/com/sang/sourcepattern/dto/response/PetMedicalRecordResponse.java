package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetMedicalRecordResponse {
    int id;
    int petId;
    Integer bookingId;
    Integer staffId;
    String staffName;
    String shopName;
    String diagnosis;
    String treatment;
    String prescription;
    LocalDateTime visitDate;
    String veterinarianNote;
}
