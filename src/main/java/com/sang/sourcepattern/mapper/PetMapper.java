package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.request.*;
import com.sang.sourcepattern.dto.response.PetDocumentResponse;
import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.entity.Pet;
import com.sang.sourcepattern.entity.PetDocument;
import com.sang.sourcepattern.entity.PetMeal;
import com.sang.sourcepattern.entity.PetMedicalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PetMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "medicalRecords", ignore = true)
    Pet toPet(PetCreationRequest request);

    @Mapping(target = "ownerFullName", source = "owner.fullName")
    PetResponse toPetResponse(Pet pet);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "medicalRecords", ignore = true)
    @Mapping(target = "nutritionPlan", ignore = true) // Handle separately in service if needed, or use automatic mapping if possible
    void updatePet(@org.mapstruct.MappingTarget Pet pet, PetUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    PetMeal toPetMeal(PetMealDTO request);

    PetMealDTO toPetMealDTO(PetMeal petMeal);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    PetDocument toPetDocument(PetDocumentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    PetMedicalRecord toPetMedicalRecord(PetMedicalRecordDTO request);

    PetMedicalRecordDTO toPetMedicalRecordDTO(PetMedicalRecord record);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    com.sang.sourcepattern.entity.PetVaccination toPetVaccination(PetVaccinationDTO request);

    PetVaccinationDTO toPetVaccinationDTO(com.sang.sourcepattern.entity.PetVaccination record);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    com.sang.sourcepattern.entity.PetReminder toPetReminder(PetReminderDTO request);

    PetReminderDTO toPetReminderDTO(com.sang.sourcepattern.entity.PetReminder record);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pet", ignore = true)
    com.sang.sourcepattern.entity.PetImage toPetImage(PetImageDTO request);

    PetImageDTO toPetImageDTO(com.sang.sourcepattern.entity.PetImage record);

    PetDocumentResponse toPetDocumentResponse(PetDocument doc);
}
