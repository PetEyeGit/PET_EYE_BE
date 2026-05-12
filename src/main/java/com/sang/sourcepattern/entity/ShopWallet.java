package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ví của từng Shop.
 * - frozenBalance: tiền đang bị đóng băng (booking CONFIRMED/IN_PROGRESS chưa hoàn thành)
 * - availableBalance: tiền đã giải phóng (booking COMPLETED), sẵn sàng rút
 * - totalEarned: tổng tiền đã nhận từ trước đến nay (sau khi trừ phí admin)
 * - totalWithdrawn: tổng tiền đã rút thành công
 */
@Entity
@Table(name = "shop_wallet")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @OneToOne
    @JoinColumn(name = "shop_id", unique = true)
    Shop shop;

    /** Tiền đóng băng — booking chưa hoàn thành */
    @Builder.Default
    @Column(precision = 18, scale = 2, columnDefinition = "DECIMAL(18,2) DEFAULT 0.00")
    BigDecimal frozenBalance = BigDecimal.ZERO;

    /** Tiền khả dụng — booking COMPLETED, có thể rút */
    @Builder.Default
    @Column(precision = 18, scale = 2, columnDefinition = "DECIMAL(18,2) DEFAULT 0.00")
    BigDecimal availableBalance = BigDecimal.ZERO;

    /** Tổng tiền đã nhận (sau phí) */
    @Builder.Default
    @Column(precision = 18, scale = 2, columnDefinition = "DECIMAL(18,2) DEFAULT 0.00")
    BigDecimal totalEarned = BigDecimal.ZERO;

    /** Tổng tiền đã rút thành công */
    @Builder.Default
    @Column(precision = 18, scale = 2, columnDefinition = "DECIMAL(18,2) DEFAULT 0.00")
    BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();
}
