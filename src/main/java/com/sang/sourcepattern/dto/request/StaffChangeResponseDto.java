package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffChangeResponseDto {
    @NotBlank(message = "Status is required")
    String status; // ACCEPTED or REJECTED
}
