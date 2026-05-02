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
    /** MANUAL | OPEN_POOL | AUTO */
    String assignmentMode;
}
