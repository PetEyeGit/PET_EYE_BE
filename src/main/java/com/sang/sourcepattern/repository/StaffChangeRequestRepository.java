package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.StaffChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffChangeRequestRepository extends JpaRepository<StaffChangeRequest, Integer> {
    List<StaffChangeRequest> findByBookingId(int bookingId);
    List<StaffChangeRequest> findByBookingIdAndStatus(int bookingId, String status);
}
