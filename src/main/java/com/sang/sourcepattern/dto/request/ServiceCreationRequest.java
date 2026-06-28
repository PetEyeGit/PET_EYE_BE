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
public class ServiceCreationRequest {

    @NotBlank(message = "SERVICE_NAME_REQUIRED")
    String serviceName;

    @NotBlank(message = "SERVICE_CATEGORY_REQUIRED")
    String category;

    @NotNull(message = "SERVICE_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", inclusive = false, message = "SERVICE_PRICE_INVALID")
    BigDecimal price;

    @Min(value = 1, message = "SERVICE_DURATION_INVALID")
    int durationMinutes;

    @Size(min = 10, message = "SERVICE_DESCRIPTION_TOO_SHORT")
    String description;

    String imageUrl;

    // ── BOARDING-only camera config ──────────────────────────────────────────
    boolean cameraEnabled;
    /** List of supported tiers, e.g. ["BASIC","HD","AI"] */
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
