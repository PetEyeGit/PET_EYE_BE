package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.PetDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetDocumentRepository extends JpaRepository<PetDocument, Integer> {
    List<PetDocument> findByPetId(int petId);
}
