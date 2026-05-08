package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareLogRequest {
    @NotBlank(message = "Log type is required")
    String type;
    
    @NotBlank(message = "Note is required")
    String note;
    
    String imageUrl;
}
