package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.PetMedicalRecordDTO;
import com.sang.sourcepattern.dto.request.PetVaccinationDTO;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.PetMedicalRecordResponse;
import com.sang.sourcepattern.dto.response.PetVaccinationResponse;
import com.sang.sourcepattern.service.MedicalRecordService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/medical")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordController {

    MedicalRecordService medicalRecordService;

    @PostMapping("/booking/{bookingId}")
    public ApiResponse<PetMedicalRecordResponse> addMedicalRecord(@PathVariable int bookingId, @RequestBody PetMedicalRecordDTO request, @AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        String email = jwt.getClaim("email");
        return ApiResponse.<PetMedicalRecordResponse>builder()
                .result(medicalRecordService.addMedicalRecord(bookingId, request, email))
                .build();
    }

    @PostMapping("/vaccinations/booking/{bookingId}")
    public ApiResponse<PetVaccinationResponse> addVaccination(@PathVariable int bookingId, @RequestBody PetVaccinationDTO request, @AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        String email = jwt.getClaim("email");
        return ApiResponse.<PetVaccinationResponse>builder()
                .result(medicalRecordService.addVaccination(bookingId, request, email))
                .build();
    }

    @GetMapping("/pet/{petId}")
    public ApiResponse<List<PetMedicalRecordResponse>> getMedicalRecordsByPet(@PathVariable int petId) {
        return ApiResponse.<List<PetMedicalRecordResponse>>builder()
                .result(medicalRecordService.getMedicalRecordsByPet(petId))
                .build();
    }

    @GetMapping("/vaccinations/pet/{petId}")
    public ApiResponse<List<PetVaccinationResponse>> getVaccinationsByPet(@PathVariable int petId) {
        return ApiResponse.<List<PetVaccinationResponse>>builder()
                .result(medicalRecordService.getVaccinationsByPet(petId))
                .build();
    }
}
