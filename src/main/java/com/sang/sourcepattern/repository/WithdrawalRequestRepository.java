package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Integer> {

    List<WithdrawalRequest> findByShopIdOrderByCreatedAtDesc(int shopId);

    List<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();

    Optional<WithdrawalRequest> findByPayosOrderCode(Long payosOrderCode);

    /** Tổng tiền đang chờ duyệt + đang chuyển của 1 shop (để chặn tạo yêu cầu mới) */
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w " +
           "WHERE w.shop.id = :shopId AND w.status IN ('PENDING', 'PAYING')")
    BigDecimal sumPendingByShop(@Param("shopId") int shopId);

    /** Tìm tất cả PAYING đã quá :cutoff (dùng cho auto-expire job) */
    @Query("SELECT w FROM WithdrawalRequest w " +
           "WHERE w.status = 'PAYING' AND w.createdAt < :cutoff")
    List<WithdrawalRequest> findStalePayouts(@Param("cutoff") LocalDateTime cutoff);
}
