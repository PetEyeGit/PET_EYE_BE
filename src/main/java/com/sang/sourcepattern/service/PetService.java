package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.PetCreationRequest;
import com.sang.sourcepattern.dto.response.PetResponse;

import java.util.List;

public interface PetService {
    PetResponse createPet(PetCreationRequest request);
    List<PetResponse> getPetsByOwner(int ownerId);
    PetResponse getPet(int id);
    PetResponse updatePet(int id, com.sang.sourcepattern.dto.request.PetUpdateRequest request);
    void deletePet(int id, String reason);
    void addAlbumImageFromCareLog(int petId, int careLogId);
}
