package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopUpdateRequest {
    String shopName;
    String shopType;
    String email;
    String phone;
    String address;
    String city;
    String description;
    String openTime;
    String closeTime;
    String workingDays;
    String licenseImageUrl;
    String logoUrl;
    String bannerUrl;
    String galleryUrls;
    /** MANUAL | OPEN_POOL | AUTO */
    String assignmentMode;
    /** Số phút châm chước No-Show (5–30). Null = không thay đổi */
    Integer lateGracePeriod;
}
