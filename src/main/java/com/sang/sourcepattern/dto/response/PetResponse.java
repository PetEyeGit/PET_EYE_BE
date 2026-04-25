package com.sang.sourcepattern.dto.response;

import com.sang.sourcepattern.dto.request.PetMealDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

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
    String gender;
    String color;
    String avatar;
    boolean sterilized;
    float weight;
    LocalDate dob;
    String healthNote;
    String favoriteFood;
    String allergies;
    String hobbies;
    String walkTime;
    List<PetMealDTO> nutritionPlan;
    String ownerFullName;
    boolean active;
    String unactiveReason;

    java.util.List<PetDocumentResponse> documents;
    java.util.List<com.sang.sourcepattern.dto.request.PetMedicalRecordDTO> medicalRecords;
    java.util.List<com.sang.sourcepattern.dto.request.PetVaccinationDTO> vaccinations;
    java.util.List<com.sang.sourcepattern.dto.request.PetReminderDTO> reminders;
    java.util.List<com.sang.sourcepattern.dto.request.PetImageDTO> album;
}
