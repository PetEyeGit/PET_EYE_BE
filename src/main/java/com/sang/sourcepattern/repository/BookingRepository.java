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

    /** Tasks assigned to a specific staff member */
    List<Booking> findByStaffId(int staffId);

    /** Bookings in a shop that have not been assigned to any staff */
    List<Booking> findByShopIdAndStaffIsNull(int shopId);

    List<Booking> findByShopIdAndAppointmentDatetimeBetween(int shopId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
