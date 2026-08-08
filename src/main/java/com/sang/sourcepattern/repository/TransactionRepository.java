package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByShopIdOrderByCreatedAtDesc(int shopId);
    Page<Transaction> findByShopIdOrderByCreatedAtDesc(int shopId, Pageable pageable);

    List<Transaction> findByBookingIdOrderByCreatedAtDesc(int bookingId);
    boolean existsByBookingIdAndType(int bookingId, String type);

    Optional<Transaction> findByPayosOrderCode(Long payosOrderCode);

    List<Transaction> findByWithdrawalIdOrderByCreatedAtDesc(int withdrawalId);

    @Query("SELECT t FROM Transaction t WHERE t.booking.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByBookingUserIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userId") int userId);
    
    @Query("SELECT t FROM Transaction t WHERE t.booking.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByBookingUserIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userId") int userId, Pageable pageable);

    /** Tổng doanh thu shop (WALLET_CREDIT SUCCESS) */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.shop.id = :shopId AND t.type = 'WALLET_CREDIT' AND t.status = 'SUCCESS'")
    BigDecimal sumWalletCreditByShop(int shopId);

    /** Tổng doanh thu toàn hệ thống */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.type = 'BOOKING_PAYMENT' AND t.status = 'SUCCESS'")
    BigDecimal sumTotalRevenue();

    @Query("""
        SELECT DATE(t.createdAt), COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE YEAR(t.createdAt) = :year AND MONTH(t.createdAt) = :month
          AND t.status = 'SUCCESS'
        GROUP BY DATE(t.createdAt)
    """)
    List<Object[]> transactionVolumeByDateRange(@org.springframework.data.repository.query.Param("year") int year, @org.springframework.data.repository.query.Param("month") int month);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (:shopId IS NULL OR t.shop.id = :shopId OR (t.booking IS NOT NULL AND t.booking.shop.id = :shopId))
          AND (:status IS NULL OR :status = '' OR UPPER(t.status) = UPPER(:status) OR (:status = 'FAILED' AND UPPER(t.status) IN ('CANCELLED', 'FAILED', 'REJECTED', 'EXPIRED')))
          AND (:type IS NULL OR :type = '' OR UPPER(t.type) = UPPER(:type))
          AND (:search IS NULL OR :search = '' OR 
               LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) OR 
               (t.booking IS NOT NULL AND LOWER(t.booking.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) OR 
               (t.booking IS NOT NULL AND LOWER(t.booking.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) OR
               (t.booking IS NOT NULL AND CAST(t.booking.id AS string) LIKE CONCAT('%', :search, '%')))
        ORDER BY t.createdAt DESC
    """)
    Page<Transaction> searchTransactionsForAdmin(
            @org.springframework.data.repository.query.Param("shopId") Integer shopId,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("type") String type,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Transaction t SET t.description = CONCAT('Dat coc ', MOD(t.payosOrderCode, 100000)) " +
           "WHERE t.payosOrderCode IS NOT NULL AND UPPER(t.paymentMethod) = 'CASH_DEPOSIT' AND " +
           "(t.description IS NULL OR t.description LIKE '%deposit paid%' OR t.description LIKE '%Thanh toán qua PayOS%' OR t.description LIKE '%Đã thanh toán cọc%' OR t.description LIKE '%Thu tiền mặt tại quầy%')")
    int standardizeOldCashDepositDescriptions();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Transaction t SET t.description = CONCAT('Thanh toan ', MOD(t.payosOrderCode, 100000)) " +
           "WHERE t.payosOrderCode IS NOT NULL AND UPPER(t.paymentMethod) != 'CASH_DEPOSIT' AND " +
           "(t.description IS NULL OR t.description LIKE '%deposit paid%' OR t.description LIKE '%Thanh toán qua PayOS%' OR t.description LIKE '%Đã thanh toán cọc%' OR t.description LIKE '%Thu tiền mặt tại quầy%')")
    int standardizeOldPayosDescriptions();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Transaction t SET t.description = CONCAT('Cash collected at venue for booking #', t.booking.id) " +
           "WHERE t.payosOrderCode IS NULL AND t.booking IS NOT NULL AND " +
           "(t.description IS NULL OR t.description LIKE '%Thu tiền mặt tại quầy%' OR t.description LIKE '%Thanh toán%')")
    int standardizeOldVenueCashDescriptions();
}

