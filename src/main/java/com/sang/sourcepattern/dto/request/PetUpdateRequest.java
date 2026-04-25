package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetUpdateRequest {
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
    java.util.List<PetMealDTO> nutritionPlan;
    java.util.List<PetMedicalRecordDTO> medicalRecords;
    java.util.List<PetVaccinationDTO> vaccinations;
    java.util.List<PetReminderDTO> reminders;
    java.util.List<PetImageDTO> album;
}
