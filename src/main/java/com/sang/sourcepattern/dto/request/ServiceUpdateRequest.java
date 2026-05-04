package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceUpdateRequest {

    String serviceName;

    String category;

    @DecimalMin(value = "0.0", inclusive = false, message = "SERVICE_PRICE_INVALID")
    BigDecimal price;

    @Min(value = 1, message = "SERVICE_DURATION_INVALID")
    Integer durationMinutes;

    @Size(min = 10, message = "SERVICE_DESCRIPTION_TOO_SHORT")
    String description;

    String imageUrl;

    Boolean active;

    // ── BOARDING-only camera config ──────────────────────────────────────────
    Boolean cameraEnabled;
    java.util.List<String> cameraTiers;
    /** Custom prices per tier (extra VND/day), e.g. {"BASIC":0,"HD":60000} */
    java.util.Map<String, Integer> cameraTierPrices;
    /** Custom display labels per tier, e.g. {"BASIC":"Tiêu chuẩn","HD":"Nét cao"} */
    java.util.Map<String, String> cameraTierLabels;
    String cameraDescription;
}
