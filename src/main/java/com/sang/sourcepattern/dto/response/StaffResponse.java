package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffResponse {
    int id;
    int shopId;
    Integer userId;
    String email;
    String fullName;
    String role;
    String phone;
    String specialization;
    boolean isActive;
}
