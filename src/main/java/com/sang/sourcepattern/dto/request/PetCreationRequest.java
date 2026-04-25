package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetCreationRequest {
    String name;
    String species;
    String breed;
    float weight;
    LocalDate dob;
    String healthNote;
    int ownerId;

    List<PetDocumentRequest> initialDocuments;
}
