package com.sang.sourcepattern.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSCreateResponse {
    String code;
    String desc;
    PayOSData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayOSData {
        String bin;
        String accountNumber;
        String accountName;
        Integer amount;
        String description;
        Long orderCode;
        String currency;
        String paymentLinkId;
        String status;
        String checkoutUrl;
        String qrCode;
    }

    public boolean isSuccess() {
        return "00".equals(code);
    }
}
