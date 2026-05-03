package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.StaffCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffCertificateRepository extends JpaRepository<StaffCertificate, Integer> {
    List<StaffCertificate> findByStaffId(int staffId);
}
