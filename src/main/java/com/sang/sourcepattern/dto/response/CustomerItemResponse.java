package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerItemResponse {
    int id;
    String name;
    String email;
    String phone;
    String avatar;
    int pets;
    int totalBookings;
    String totalSpent;
    String lastVisit;
    String tier; // NEW, REGULAR, VIP
}
