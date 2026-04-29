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

    // URL returned from /files/upload (Cloudinary)
    String imageUrl;
}
