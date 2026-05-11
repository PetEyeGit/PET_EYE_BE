package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopWalletResponse {
    int id;
    int shopId;
    BigDecimal frozenBalance;
    BigDecimal availableBalance;
    BigDecimal totalEarned;
    BigDecimal totalWithdrawn;
    String updatedAt;
}
