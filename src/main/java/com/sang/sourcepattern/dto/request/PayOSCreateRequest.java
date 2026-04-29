package com.sang.sourcepattern.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayOSCreateRequest {
    Long orderCode;
    Integer amount;
    String description;
    String buyerName;
    String buyerEmail;
    String buyerPhone;
    String buyerAddress;
    List<PayOSItem> items;
    String cancelUrl;
    String returnUrl;
    Long expiredAt;
    String signature;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayOSItem {
        String name;
        Integer quantity;
        Integer price;
    }
}
