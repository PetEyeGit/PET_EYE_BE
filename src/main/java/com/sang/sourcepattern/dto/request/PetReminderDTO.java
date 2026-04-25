package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetReminderDTO {
    String title;
    String description;
    LocalDateTime date;
    String type;
    String status;
}
