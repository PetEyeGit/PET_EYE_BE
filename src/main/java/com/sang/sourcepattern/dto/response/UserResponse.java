package com.sang.sourcepattern.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    long id;
    String email;
    String fullName;
    String phone;
    String address;
    String avatar;
    boolean active;
    Set<RoleResponse> roles;

}