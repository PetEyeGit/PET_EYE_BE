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
    String shopType;
    String email;
    String phone;
    String address;
    String city;
    String description;
    String licenseNumber;
    String licenseImageUrl;
    String logoUrl;
    String bannerUrl;
    String galleryUrls;
    java.util.List<StaffResponse> staffs;
    String openTime;
    String closeTime;
    String workingDays;
    boolean isVerified;
    float ratingAvg;
    int ownerId;
    /** MANUAL | OPEN_POOL | AUTO */
    String assignmentMode;
}
