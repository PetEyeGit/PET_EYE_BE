package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.PetVaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetVaccinationRepository extends JpaRepository<PetVaccination, Integer> {
    List<PetVaccination> findByPetIdOrderByDateDesc(Integer petId);
}
