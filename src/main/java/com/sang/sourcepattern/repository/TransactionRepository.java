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
}
