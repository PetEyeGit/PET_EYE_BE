package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByUserId(int userId);
    List<Booking> findByShopId(int shopId);
    Optional<Booking> findByPayosOrderCode(Long payosOrderCode);
}
