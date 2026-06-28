package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffUpdateRequest {
    @NotBlank(message = "FULL_NAME_REQUIRED")
    String fullName;
    
    String phone;
    String role;
    String specialization;
    String avatar;
}
