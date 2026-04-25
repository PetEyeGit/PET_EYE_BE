package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.PetCreationRequest;
import com.sang.sourcepattern.dto.request.PetDocumentRequest;
import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.entity.Pet;
import com.sang.sourcepattern.entity.PetDocument;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.mapper.PetMapper;
import com.sang.sourcepattern.repository.PetDocumentRepository;
import com.sang.sourcepattern.repository.PetRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.PetService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PetServiceImpl implements PetService {
    PetRepository petRepository;
    UserRepository userRepository;
    PetDocumentRepository petDocumentRepository;
    PetMapper petMapper;

    @Override
    @Transactional
    public PetResponse createPet(PetCreationRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pet pet = petMapper.toPet(request);
        pet.setOwner(owner);
        Pet savedPet = petRepository.save(pet);

        if (request.getInitialDocuments() != null) {
            List<PetDocument> docs = request.getInitialDocuments().stream()
                    .map(docReq -> {
                        PetDocument doc = petMapper.toPetDocument(docReq);
                        doc.setPet(savedPet);
                        return doc;
                    })
                    .toList();
            petDocumentRepository.saveAll(docs);
            savedPet.setDocuments(docs);
        }

        return petMapper.toPetResponse(savedPet);
    }

    @Override
    public List<PetResponse> getPetsByOwner(int ownerId) {
        return petRepository.findByOwnerId(ownerId).stream()
                .map(petMapper::toPetResponse)
                .toList();
    }

    @Override
    public PetResponse getPet(int id) {
        return petRepository.findById(id)
                .map(petMapper::toPetResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
    }

    @Override
    public void deletePet(int id, String reason) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
        
        pet.setActive(false);
        pet.setUnactiveReason(reason);
        petRepository.save(pet);
    }
}
