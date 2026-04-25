package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.request.PetCreationRequest;
import com.sang.sourcepattern.dto.request.PetDocumentRequest;
import com.sang.sourcepattern.dto.response.PetDocumentResponse;
import com.sang.sourcepattern.dto.response.PetResponse;
import com.sang.sourcepattern.entity.Pet;
import com.sang.sourcepattern.entity.PetDocument;
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
    @Mapping(target = "pet", ignore = true)
    PetDocument toPetDocument(PetDocumentRequest request);

    PetDocumentResponse toPetDocumentResponse(PetDocument doc);
}
