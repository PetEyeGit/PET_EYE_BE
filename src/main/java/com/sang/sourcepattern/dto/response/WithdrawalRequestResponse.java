package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalRequestResponse {
    int id;
    int shopId;
    String shopName;
    BigDecimal amount;
    String bankName;
    String bankAccount;
    String accountHolder;
    String note;
    String status;
    String adminNote;
    Long payosOrderCode;
    String checkoutUrl;
    String createdAt;
    String processedAt;
    String type;
}
