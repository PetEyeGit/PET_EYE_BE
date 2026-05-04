package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Integer> {
    List<Pet> findByOwnerId(int ownerId);
    long countByOwnerId(int ownerId);
}
