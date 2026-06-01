package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    /** Load bookings with services eagerly using JOIN FETCH to avoid empty services collection */
    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.user.id = :userId")
    List<Booking> findByUserIdWithServices(@Param("userId") int userId);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.shop.id = :shopId")
    List<Booking> findByShopIdWithServices(@Param("shopId") int shopId);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.payosOrderCode = :orderCode")
    Optional<Booking> findByPayosOrderCodeWithServices(@Param("orderCode") Long orderCode);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.id = :id")
    Optional<Booking> findByIdWithServices(@Param("id") int id);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.staff.id = :staffId")
    List<Booking> findByStaffIdWithServices(@Param("staffId") int staffId);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.shop.id = :shopId AND b.staff IS NULL")
    List<Booking> findByShopIdAndStaffIsNullWithServices(@Param("shopId") int shopId);

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.shop.id = :shopId AND b.appointmentDatetime BETWEEN :start AND :end")
    List<Booking> findByShopIdAndDatetimeBetweenWithServices(@Param("shopId") int shopId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Keep old methods for backward compatibility
    List<Booking> findByUserId(int userId);
    List<Booking> findByShopId(int shopId);
    Optional<Booking> findByPayosOrderCode(Long payosOrderCode);

    /** Tong doanh thu toan he thong tu bookings (CONFIRMED / IN_PROGRESS / COMPLETED) */
    @Query("SELECT COALESCE(SUM(s.price), 0) FROM Booking b JOIN b.services s WHERE b.status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED')")
    BigDecimal sumTotalRevenue();

    @Query("SELECT DISTINCT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.status = 'COMPLETED'")
    List<Booking> findCompletedBookingsWithServices();

    /** Tong doanh thu cua cac booking da hoan thanh */
    @Query("SELECT COALESCE(SUM(s.price), 0) FROM Booking b JOIN b.services s WHERE b.status = 'COMPLETED'")
    BigDecimal sumTotalCompletedRevenue();

    /** Tasks assigned to a specific staff member */
    List<Booking> findByStaffId(int staffId);

    /** Bookings in a shop that have not been assigned to any staff */
    List<Booking> findByShopIdAndStaffIsNull(int shopId);

    List<Booking> findByShopIdAndAppointmentDatetimeBetween(int shopId, LocalDateTime start, LocalDateTime end);

    /** Doanh thu theo thang trong nam cho shop (sau khi tru phi admin tuong ung) cho booking COMPLETED */
    @Query("""
        SELECT MONTH(b.appointmentDatetime),
               COALESCE(SUM(
                   s.price * (
                       CASE 
                           WHEN (LOWER(s.category) LIKE '%grooming%' OR LOWER(s.category) LIKE '%spa%') THEN 0.82
                           WHEN (LOWER(s.category) LIKE '%boarding%' OR LOWER(s.category) LIKE '%hotel%') AND s.cameraEnabled = true THEN 0.75
                           WHEN LOWER(s.category) LIKE '%clinic%' THEN 0.90
                           ELSE 0.90
                       END
                   )
               ), 0)
        FROM Booking b JOIN b.services s
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
     * Kiểm tra pet đã có booking active (CONFIRMED / IN_PROGRESS) bị overlap không.
     * Overlap: existingStart < newEnd AND existingStart + duration > newStart
     * Truyền vào: windowStart = newStart, windowEnd = newStart + newDuration
     * existingEnd được tính bằng cách truyền thêm param để JPQL so sánh.
     *
     * Vì JPQL không hỗ trợ DATE_ADD, dùng native query nhưng trả về Integer (0/1).
     */
    @Query(value = """
        SELECT COUNT(*) FROM booking b
        JOIN booking_services bs ON bs.booking_id = b.id
        JOIN pet_service s ON s.id = bs.service_id
        WHERE b.pet_id = :petId
          AND b.status IN ('WAITING_SHOP_APPROVAL', 'CONFIRMED', 'IN_PROGRESS')
          AND b.appointment_datetime < :windowEnd
          AND DATE_ADD(b.appointment_datetime, INTERVAL s.duration_minutes MINUTE) > :windowStart
          AND (
                b.shop_id != :shopId
                OR NOT (
                    (:isNewBoarding = true AND LOWER(s.category) NOT IN ('boarding', 'hotel'))
                    OR
                    (:isNewBoarding = false AND LOWER(s.category) IN ('boarding', 'hotel'))
                )
          )
    """, nativeQuery = true)
    int countConflictingBookingForPet(
            @Param("petId") int petId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd,
            @Param("shopId") int shopId,
            @Param("isNewBoarding") boolean isNewBoarding
    );

    @Query(value = """
        SELECT COUNT(*) FROM booking b
        JOIN booking_services bs ON bs.booking_id = b.id
        JOIN pet_service s ON s.id = bs.service_id
        WHERE b.pet_id = :petId
          AND b.status IN ('WAITING_SHOP_APPROVAL', 'CONFIRMED', 'IN_PROGRESS')
          AND b.appointment_datetime < :windowEnd
          AND DATE_ADD(b.appointment_datetime, INTERVAL s.duration_minutes MINUTE) > :windowStart
    """, nativeQuery = true)
    int countStrictConflictingBookingForPet(
            @Param("petId") int petId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    /**
     * Kiểm tra staff đã có booking active trong khung giờ đó chưa.
     */
    @Query(value = """
        SELECT COUNT(*) FROM booking b
        JOIN booking_services bs ON bs.booking_id = b.id
        JOIN pet_service s ON s.id = bs.service_id
        WHERE b.staff_id = :staffId
          AND b.status IN ('WAITING_SHOP_APPROVAL', 'CONFIRMED', 'IN_PROGRESS')
          AND LOWER(s.category) NOT IN ('boarding', 'hotel')
          AND b.appointment_datetime < :windowEnd
          AND DATE_ADD(b.appointment_datetime, INTERVAL s.duration_minutes MINUTE) > :windowStart
    """, nativeQuery = true)
    int countConflictingBookingForStaff(
            @Param("staffId") int staffId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    @Query("SELECT DISTINCT b.shop.id FROM Booking b WHERE b.user.email = :email")
    List<Integer> findShopIdsByUserEmail(@Param("email") String email);
}
