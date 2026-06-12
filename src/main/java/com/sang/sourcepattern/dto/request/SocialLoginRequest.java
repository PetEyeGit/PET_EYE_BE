package com.sang.sourcepattern.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocialLoginRequest {
    String token;        // for Google/Facebook (access_token or id_token)
    String code;         // for Zalo – authorization code
    String codeVerifier; // for Zalo – PKCE verifier (required by Zalo OAuth v4)
}
