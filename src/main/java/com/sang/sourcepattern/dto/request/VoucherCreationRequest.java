package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherCreationRequest {
    String code;
    String targetTierName;
    Double requiredSpending;
    String discountType;
    Double discountValue;
    Double minOrderValue;
    Integer validDays;
    Integer issueQuantity;

    /**
     * Lo\u1ea1i voucher: "TIER" (m\u1eb7c \u0111\u1ecbnh) ho\u1eb7c "NEWCOMER" (t\u00e2n th\u1ee7).
     * M\u1eb7c \u0111\u1ecbnh l\u00e0 "TIER" n\u1ebfu kh\u00f4ng truy\u1ec1n.
     */
    @Builder.Default
    String voucherType = "TIER";

    /**
     * Category d\u1ecbch v\u1ee5 \u0111\u01b0\u1ee3c \u00e1p d\u1ee5ng (ch\u1ec9 cho NEWCOMER).
     * null = m\u1ecdi d\u1ecbch v\u1ee5; "SPA", "GROOMING", ... = gi\u1edbi h\u1ea1n category.
     */
    String targetServiceCategory;
}
