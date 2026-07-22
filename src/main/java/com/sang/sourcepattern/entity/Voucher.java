package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(unique = true, nullable = false)
    String code;

    // e.g., "PERCENTAGE" or "FIXED_AMOUNT"
    @Column(nullable = false)
    String discountType;

    @Column(nullable = false)
    Double discountValue;

    Double minOrderValue;
    Double maxDiscountAmount;

    @Builder.Default
    Integer validDays = 30;

    @Builder.Default
    Integer issueQuantity = 1;

    /**
     * - "TIER"     : phát theo hạng thành viên (mặc định cũ)
     * - "NEWCOMER" : phát cho người mới đăng ký tài khoản
     */
    @Column(nullable = false)
    @Builder.Default
    String voucherType = "TIER";

    @Transient
    Long issuedQuantity;

    /**
     * Giới hạn áp dụng theo category dịch vụ.
     * null = áp dụng mọi dịch vụ; "SPA", "GROOMING", ... = giới hạn category.
     */
    String targetServiceCategory;

    /**
     * Bật/tắt từng voucher riêng lẻ.
     * true = voucher đang hoạt động; false = voucher bị tạm ngừng.
     */
    @Builder.Default
    @Column(nullable = false)
    boolean active = true;

    @ManyToOne
    @JoinColumn(name = "target_tier_id")
    MembershipTier targetTier;
}
