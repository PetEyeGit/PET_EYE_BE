package com.sang.sourcepattern.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    String avatar;
    @JsonProperty("isActive")
    boolean isActive;

    java.util.List<StaffCertificateResponse> certificates;
}
