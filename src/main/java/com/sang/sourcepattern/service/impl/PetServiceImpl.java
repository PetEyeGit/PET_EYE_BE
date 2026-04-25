package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.*;
import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.entity.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
        pet.setActive(true);

        if (request.getNutritionPlan() != null) {
            pet.setNutritionPlan(request.getNutritionPlan().stream()
                    .map(dto -> {
                        PetMeal meal = petMapper.toPetMeal(dto);
                        meal.setPet(pet);
                        return meal;
                    }).collect(Collectors.toList()));
        }

        if (request.getMedicalRecords() != null) {
            pet.setMedicalRecords(request.getMedicalRecords().stream()
                    .map(dto -> {
                        PetMedicalRecord record = petMapper.toPetMedicalRecord(dto);
                        record.setPet(pet);
                        return record;
                    }).collect(Collectors.toList()));
        }

        if (request.getVaccinations() != null) {
            pet.setVaccinations(request.getVaccinations().stream()
                    .map(dto -> {
                        PetVaccination v = petMapper.toPetVaccination(dto);
                        v.setPet(pet);
                        return v;
                    }).collect(Collectors.toList()));
        }

        if (request.getReminders() != null) {
            pet.setReminders(request.getReminders().stream()
                    .map(dto -> {
                        PetReminder r = petMapper.toPetReminder(dto);
                        r.setPet(pet);
                        return r;
                    }).collect(Collectors.toList()));
        }

        if (request.getAlbum() != null) {
            pet.setAlbum(request.getAlbum().stream()
                    .map(dto -> {
                        PetImage img = petMapper.toPetImage(dto);
                        img.setPet(pet);
                        return img;
                    }).collect(Collectors.toList()));
        }

        Pet savedPet = petRepository.save(pet);

        // Documents are a bit different because they might use a separate repository call in the original code
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
    public PetResponse updatePet(int id, PetUpdateRequest request) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
        
        petMapper.updatePet(pet, request);

        if (request.getNutritionPlan() != null) {
            // Force clearing of the old plan and flush to delete orphans
            if (pet.getNutritionPlan() != null) {
                pet.getNutritionPlan().clear();
                petRepository.saveAndFlush(pet);
            } else {
                pet.setNutritionPlan(new ArrayList<>());
            }
            
            for (PetMealDTO dto : request.getNutritionPlan()) {
                PetMeal meal = petMapper.toPetMeal(dto);
                meal.setPet(pet);
                pet.getNutritionPlan().add(meal);
            }
        }

        if (request.getMedicalRecords() != null) {
            // Force clearing of the old records and flush to delete orphans
            if (pet.getMedicalRecords() != null) {
                pet.getMedicalRecords().clear();
                petRepository.saveAndFlush(pet);
            } else {
                pet.setMedicalRecords(new ArrayList<>());
            }

            for (PetMedicalRecordDTO dto : request.getMedicalRecords()) {
                PetMedicalRecord record = petMapper.toPetMedicalRecord(dto);
                record.setPet(pet);
                pet.getMedicalRecords().add(record);
            }
        }

        if (request.getVaccinations() != null) {
            if (pet.getVaccinations() != null) {
                pet.getVaccinations().clear();
                petRepository.saveAndFlush(pet);
            } else {
                pet.setVaccinations(new ArrayList<>());
            }
            for (PetVaccinationDTO dto : request.getVaccinations()) {
                PetVaccination v = petMapper.toPetVaccination(dto);
                v.setPet(pet);
                pet.getVaccinations().add(v);
            }
        }

        if (request.getReminders() != null) {
            if (pet.getReminders() != null) {
                pet.getReminders().clear();
                petRepository.saveAndFlush(pet);
            } else {
                pet.setReminders(new ArrayList<>());
            }
            for (PetReminderDTO dto : request.getReminders()) {
                PetReminder r = petMapper.toPetReminder(dto);
                r.setPet(pet);
                pet.getReminders().add(r);
            }
        }

        if (request.getAlbum() != null) {
            if (pet.getAlbum() != null) {
                pet.getAlbum().clear();
                petRepository.saveAndFlush(pet);
            } else {
                pet.setAlbum(new ArrayList<>());
            }
            for (PetImageDTO dto : request.getAlbum()) {
                PetImage img = petMapper.toPetImage(dto);
                img.setPet(pet);
                pet.getAlbum().add(img);
            }
        }

        Pet savedPet = petRepository.save(pet);
        return petMapper.toPetResponse(savedPet);
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
