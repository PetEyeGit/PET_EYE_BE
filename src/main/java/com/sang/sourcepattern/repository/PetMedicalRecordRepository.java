package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.PetMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetMedicalRecordRepository extends JpaRepository<PetMedicalRecord, Integer> {
    List<PetMedicalRecord> findByPetIdOrderByVisitDateDesc(Integer petId);
    int countByBookingId(Integer bookingId);
}
