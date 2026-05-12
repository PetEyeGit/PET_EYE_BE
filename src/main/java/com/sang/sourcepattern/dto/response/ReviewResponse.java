package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    int id;
    String userName;
    String userAvatar;
    String serviceName;
    int rating;
    String comment;
    LocalDateTime createdAt;
    String reply;
    LocalDateTime repliedAt;
}
