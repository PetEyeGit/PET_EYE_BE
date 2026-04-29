package com.sang.sourcepattern.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentInfoResponse {
    String code;
    String desc;
    PayOSPaymentData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayOSPaymentData {
        String id;
        Long orderCode;
        Integer amount;
        Integer amountPaid;
        Integer amountRemaining;
        String status;   // PENDING, PAID, CANCELLED, EXPIRED
        String createdAt;
        String canceledAt;
        String cancellationReason;
        String checkoutUrl;
        String qrCode;
    }

    public boolean isSuccess() {
        return "00".equals(code);
    }

    public String getPaymentStatus() {
        return data != null ? data.getStatus() : null;
    }
}
