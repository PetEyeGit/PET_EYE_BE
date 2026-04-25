package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetResponse {
    int id;
    String name;
    String species;
    String breed;
    float weight;
    LocalDate dob;
    String healthNote;
    String ownerFullName;
    boolean isActive;
    String unactiveReason;

    java.util.List<PetDocumentResponse> documents;
}
