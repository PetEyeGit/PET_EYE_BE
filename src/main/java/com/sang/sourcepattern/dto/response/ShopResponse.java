package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopResponse {
    int id;
    String shopName;
    String address;
    String city;
    String description;
    String licenseNumber;
    float ratingAvg;
    boolean isVerified;
    String ownerFullName;
}
