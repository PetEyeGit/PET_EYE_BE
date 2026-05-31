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

    @ManyToOne
    @JoinColumn(name = "target_tier_id")
    MembershipTier targetTier;
}
