package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByPayosOrderCode(Long payosOrderCode);
    Optional<Payment> findByBookingId(int bookingId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.booking.shop.id = :shopId")
    BigDecimal sumRevenueByShop(int shopId);
}
