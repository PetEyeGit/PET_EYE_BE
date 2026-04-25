package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.PetCreationRequest;
import com.sang.sourcepattern.dto.request.PetUpdateRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Pet Management", description = "Endpoints for managing pets")
public class PetController {
    PetService petService;

    @PostMapping
    @Operation(summary = "Add a new pet")
    ApiResponse<PetResponse> createPet(@RequestBody @Valid PetCreationRequest request) {
        return ApiResponse.<PetResponse>builder()
                .result(petService.createPet(request))
                .build();
    }

    @GetMapping("/owner/{ownerId}")
    ApiResponse<List<PetResponse>> getPetsByOwner(@PathVariable int ownerId) {
        return ApiResponse.<List<PetResponse>>builder()
                .result(petService.getPetsByOwner(ownerId))
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<PetResponse> getPet(@PathVariable int id) {
        return ApiResponse.<PetResponse>builder()
                .result(petService.getPet(id))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update pet information")
    ApiResponse<PetResponse> updatePet(@PathVariable int id, @RequestBody @Valid PetUpdateRequest request) {
        return ApiResponse.<PetResponse>builder()
                .result(petService.updatePet(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (Deactivate) a pet")
    ApiResponse<Void> deletePet(@PathVariable int id, @RequestParam String reason) {
        petService.deletePet(id, reason);
        return ApiResponse.<Void>builder()
                .message("Pet deactivated successfully. Reason: " + reason)
                .build();
    }
}
