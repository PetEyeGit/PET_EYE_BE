package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng ghi nhận mọi giao dịch tài chính trong hệ thống.
 *
 * type:
 *   BOOKING_PAYMENT  — Khách thanh toán booking (PayOS / Cash)
 *   WALLET_CREDIT    — Tiền vào ví shop sau khi booking COMPLETED
 *   WITHDRAWAL       — Shop rút tiền (payout từ admin)
 *
 * status:
 *   PENDING   — Chờ xử lý (cash chờ thu, withdrawal chờ duyệt)
 *   SUCCESS   — Thành công
 *   FAILED    — Thất bại
 *   CANCELLED — Đã huỷ
 */
@Entity
@Table(name = "transaction",
        indexes = {
                @Index(name = "idx_txn_booking", columnList = "booking_id"),
                @Index(name = "idx_txn_shop",    columnList = "shop_id"),
                @Index(name = "idx_txn_type",    columnList = "type,status"),
                @Index(name = "idx_txn_created", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    // ─── Liên kết ─────────────────────────────────────────────────────────────

    /** Booking liên quan (nullable với WITHDRAWAL) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    Booking booking;

    /** Shop nhận/gửi tiền */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    Shop shop;

    /** WithdrawalRequest liên quan (chỉ với WITHDRAWAL) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_id")
    WithdrawalRequest withdrawal;

    // ─── Thông tin giao dịch ──────────────────────────────────────────────────

    /**
     * Loại giao dịch:
     * BOOKING_PAYMENT | WALLET_CREDIT | WITHDRAWAL
     */
    @Column(nullable = false, length = 30)
    String type;

    /** Số tiền giao dịch (luôn dương) */
    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal amount;

    /**
     * Phương thức thanh toán:
     * PAYOS | CASH | WALLET
     */
    @Column(length = 20)
    String paymentMethod;

    /**
     * Trạng thái: PENDING | SUCCESS | FAILED | CANCELLED
     */
    @Builder.Default
    @Column(nullable = false, length = 20)
    String status = "PENDING";

    /** PayOS order code (nếu có) */
    Long payosOrderCode;

    /** Transaction ID từ PayOS gateway */
    @Column(length = 100)
    String gatewayTransactionId;

    /** Mô tả ngắn */
    @Column(length = 255)
    String description;

    /** Ghi chú thêm */
    @Column(columnDefinition = "TEXT")
    String note;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    LocalDateTime completedAt;
}
