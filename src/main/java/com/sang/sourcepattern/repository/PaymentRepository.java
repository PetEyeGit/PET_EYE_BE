package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPayosOrderCode(Long payosOrderCode);
    Optional<Payment> findByBookingId(int bookingId);
}
