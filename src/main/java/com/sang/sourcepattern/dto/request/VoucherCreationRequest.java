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
}
