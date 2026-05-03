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
    String cameraDescription;
}
