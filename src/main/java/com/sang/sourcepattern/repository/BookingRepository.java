package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    List<Booking> findByShopIdAndAppointmentDatetimeBetween(int shopId, LocalDateTime start, LocalDateTime end);

    /**
     * Kiểm tra pet đã có booking active (CONFIRMED / IN_PROGRESS) tại thời điểm đó chưa.
     * Dùng để ngăn 1 pet có 2 booking cùng giờ.
     * Window: [appointmentTime - durationMinutes, appointmentTime + durationMinutes]
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.pet.id = :petId
          AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
          AND b.appointmentDatetime BETWEEN :windowStart AND :windowEnd
    """)
    boolean existsConflictingBookingForPet(
            @Param("petId") int petId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );
}
