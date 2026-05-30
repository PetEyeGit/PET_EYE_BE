package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopNearbyResponse {
    int id;
    String shopName;
    String shopType;
    String address;
    String city;
    Double latitude;
    Double longitude;
    String logoUrl;
    float ratingAvg;
    Double distanceKm;
    Integer durationMinutes;
}
