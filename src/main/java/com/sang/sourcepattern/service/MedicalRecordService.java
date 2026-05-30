package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.PetMedicalRecordDTO;
import com.sang.sourcepattern.dto.request.PetVaccinationDTO;
import com.sang.sourcepattern.dto.response.PetMedicalRecordResponse;
import com.sang.sourcepattern.dto.response.PetVaccinationResponse;

import java.util.List;

public interface MedicalRecordService {
    PetMedicalRecordResponse addMedicalRecord(int bookingId, PetMedicalRecordDTO request, String userEmail);
    PetVaccinationResponse addVaccination(int bookingId, PetVaccinationDTO request, String userEmail);
    List<PetMedicalRecordResponse> getMedicalRecordsByPet(int petId);
    List<PetVaccinationResponse> getVaccinationsByPet(int petId);
}
