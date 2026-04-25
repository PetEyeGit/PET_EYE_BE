package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopRegistrationRequest {
    @NotBlank(message = "SHOP_NAME_REQUIRED")
    String shopName;

    @NotBlank(message = "SHOP_TYPE_REQUIRED")
    String shopType;

    @Email(message = "INVALID_EMAIL")
    @NotBlank(message = "EMAIL_REQUIRED")
    String email;

    @NotBlank(message = "PHONE_REQUIRED")
    String phone;

    @NotBlank(message = "ADDRESS_REQUIRED")
    String address;

    @NotBlank(message = "CITY_REQUIRED")
    String city;

    @Size(min = 10, message = "DESCRIPTION_TOO_SHORT")
    String description;

    @Size(min = 6, message = "INVALID_PASSWORD")
    String password;

    String licenseImageUrl;
}
