package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {
    List<Staff> findByShopIdAndIsActiveTrue(int shopId);
}
