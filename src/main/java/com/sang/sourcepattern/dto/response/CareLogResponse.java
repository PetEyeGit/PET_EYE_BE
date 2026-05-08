package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareLogResponse {
    int id;
    int bookingId;
    String staffName;
    String type;
    String note;
    LocalDateTime timestamp;
    String imageUrl;
}
