package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherResponse {
    Long id;
    String code;
    String targetTierName;
    Double requiredSpending;
    String discountType;
    Double discountValue;
    Integer validDays;
    Boolean isUsed;
}
