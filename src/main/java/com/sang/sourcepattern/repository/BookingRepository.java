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

    /** Doanh thu theo tháng trong năm: SUM(service.price * 0.9) cho booking COMPLETED */
    @Query("""
        SELECT MONTH(b.appointmentDatetime), COALESCE(SUM(b.service.price * 0.9), 0)
        FROM Booking b
        WHERE b.status = 'COMPLETED'
          AND YEAR(b.appointmentDatetime) = :year
        GROUP BY MONTH(b.appointmentDatetime)
    """)
    List<Object[]> revenueByMonth(@Param("year") int year);

    /** Số booking theo ngày trong khoảng thời gian, bỏ CANCELLED */
    @Query("""
        SELECT DATE(b.appointmentDatetime), COUNT(b)
        FROM Booking b
        WHERE b.appointmentDatetime >= :from
          AND b.status != 'CANCELLED'
        GROUP BY DATE(b.appointmentDatetime)
    """)
    List<Object[]> bookingCountByDate(@Param("from") LocalDateTime from);

    /**
     * Kiểm tra pet đã có booking active (CONFIRMED / IN_PROGRESS) tại thời điểm đó chưa.
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

    /**
     * Kiểm tra staff đã có booking active trong khung giờ đó chưa.
     * Window: [appointmentTime - durationMinutes, appointmentTime + durationMinutes]
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.staff.id = :staffId
          AND b.status IN ('CONFIRMED', 'IN_PROGRESS')
          AND b.appointmentDatetime BETWEEN :windowStart AND :windowEnd
    """)
    boolean existsConflictingBookingForStaff(
            @Param("staffId") int staffId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    @Query("SELECT DISTINCT b.shop.id FROM Booking b WHERE b.user.email = :email")
    List<Integer> findShopIdsByUserEmail(@Param("email") String email);
}
