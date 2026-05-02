package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskStatusUpdateRequest {

    /**
     * Allowed transitions:
     *  Staff:  CONFIRMED → IN_PROGRESS → COMPLETED
     *  Owner:  any → CANCELLED
     */
    @NotBlank(message = "Status is required")
    String status;
}
