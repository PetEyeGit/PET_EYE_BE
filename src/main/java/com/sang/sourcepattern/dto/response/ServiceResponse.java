package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceResponse {
    int id;
    int shopId;
    String shopName;
    String serviceName;
    String category;
    BigDecimal price;
    int durationMinutes;
    String description;
    String imageUrl;
    boolean active;
    LocalDateTime createdAt;

    // BOARDING-only
    boolean cameraEnabled;
    /** List of supported tiers e.g. ["BASIC","HD","AI"] */
    java.util.List<String> cameraTiers;
    /** Custom prices per tier (extra VND/day), e.g. {"BASIC":0,"HD":60000} */
    java.util.Map<String, Integer> cameraTierPrices;
    /** Custom display labels per tier, e.g. {"BASIC":"Tiêu chuẩn","HD":"Nét cao"} */
    java.util.Map<String, String> cameraTierLabels;
    String cameraDescription;
    
    java.util.List<String> cageSize;
    java.util.List<String> roomType;
    java.util.List<java.math.BigDecimal> prices;
}
