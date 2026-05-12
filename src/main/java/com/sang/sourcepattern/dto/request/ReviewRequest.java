package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewRequest {
    int shopId;
    int bookingId;
    
    @Min(1)
    @Max(5)
    int rating;
    
    @NotBlank
    String comment;
}
