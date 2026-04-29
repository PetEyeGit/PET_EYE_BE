package com.sang.sourcepattern.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sang.sourcepattern.dto.request.*;
import com.sang.sourcepattern.dto.response.AuthenticationResponse;
import com.sang.sourcepattern.dto.response.IntrospectResponse;
import com.sang.sourcepattern.entity.InvalidatedToken;
import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.InvalidatedTokenRepository;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;
    ShopRepository shopRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    InvalidatedTokenRepository invalidatedTokenRepository;
    RestTemplate restTemplate;

    @Value("${jwt.signer-key}")
    @NonFinal
    String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    @NonFinal
    long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    @NonFinal
    long REFRESHABLE_DURATION;

    @Value("${social.google.client-id:}")
    @NonFinal
    String googleClientId;

    @Value("${social.zalo.app-id:}")
    @NonFinal
    String zaloAppId;

    @Value("${social.zalo.app-secret:}")
    @NonFinal
    String zaloAppSecret;

    @Value("${social.facebook.app-id:}")
    @NonFinal
    String facebookAppId;

    @Value("${social.facebook.app-secret:}")
    @NonFinal
    String facebookAppSecret;

    @Value("${social.facebook.redirect-uri:http://localhost:3000/login/facebook/callback}")
    @NonFinal
    String facebookRedirectUri;

    // ================= LOGIN =================
    @Override
    public AuthenticationResponse authenticated(AuthenticationRequest request) {
        log.info("Authenticating user: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getEmail());
                    return new AppException(ErrorCode.USER_NOT_EXISTED);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Authentication failed for user: {}", request.getEmail());
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        if (!user.isActive()) {
            log.warn("Login attempt for deactivated account: {}", request.getEmail());
            throw new AppException(ErrorCode.ACCOUNT_DEACTIVATED);
        }

        // If SHOP_OWNER, check if their shop is verified
        boolean isShopOwner = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SHOP_OWNER"));
        
        if (isShopOwner) {
            boolean shopVerified = shopRepository.findByOwnerId(user.getId())
                    .map(shop -> shop.isVerified())
                    .orElse(false);
            
            if (!shopVerified) {
                log.warn("Login attempt for unverified shop owner: {}", request.getEmail());
                throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
            }
        }

        log.info("Login successful for user: {}", request.getEmail());

        String token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    // ================= GENERATE JWT =================
    private String generateToken(User user) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + VALID_DURATION * 1000);

            String jti = UUID.randomUUID().toString();

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getId()))
                    .claim("email", user.getEmail())
                    .claim("roles",
                            user.getRoles()
                                    .stream()
                                    .map(Role::getName)
                                    .toList())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .jwtID(jti)
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet
            );

            jwt.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Cannot generate token", e);
        }
    }

    // ================= INTROSPECT =================
    @Override
    public IntrospectResponse introspect(IntrospectRequest request)
            throws ParseException, JOSEException {

        SignedJWT jwt = SignedJWT.parse(request.getToken());

        // verify signature
        boolean signatureValid = jwt.verify(
                new MACVerifier(SIGNER_KEY.getBytes())
        );

        if (!signatureValid) {
            return IntrospectResponse.builder().valid(false).build();
        }

        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();
        if (expiry.before(new Date())) {
            return IntrospectResponse.builder().valid(false).build();
        }

        String jti = jwt.getJWTClaimsSet().getJWTID();

        // 🔥 check revoked
        if (invalidatedTokenRepository.existsById(jti)) {
            return IntrospectResponse.builder().valid(false).build();
        }

        return IntrospectResponse.builder()
                .valid(true)
                .build();
    }

    // ================= LOGOUT =================
    @Override
    public void logout(LogoutRequest request)
            throws ParseException, JOSEException {

        SignedJWT jwt = SignedJWT.parse(request.getToken());

        String jti = jwt.getJWTClaimsSet().getJWTID();
        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();

        // đã logout rồi → bỏ qua
        if (invalidatedTokenRepository.existsById(jti)) {
            return;
        }

        invalidatedTokenRepository.save(
                InvalidatedToken.builder()
                        .id(jti)
                        .expiryTime(expiry)
                        .build()
        );

        log.info("Logout success - token revoked: {}", jti);
    }

    // ================= REFRESH TOKEN =================
    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException {

        SignedJWT jwt = SignedJWT.parse(request.getToken());

        String jti = jwt.getJWTClaimsSet().getJWTID();
        Date issuedAt = jwt.getJWTClaimsSet().getIssueTime();
        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();

        // ❌ token đã logout
        if (invalidatedTokenRepository.existsById(jti)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // ❌ quá hạn refresh
        long refreshDeadline =
                issuedAt.getTime() + REFRESHABLE_DURATION * 1000;

        if (System.currentTimeMillis() > refreshDeadline) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String email = jwt.getJWTClaimsSet().getStringClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        // 🔥 revoke token cũ
        invalidatedTokenRepository.save(
                InvalidatedToken.builder()
                        .id(jti)
                        .expiryTime(expiry)
                        .build()
        );

        String newToken = generateToken(user);

        return AuthenticationResponse.builder()
                .token(newToken)
                .authenticated(true)
                .build();
    }

    // ================= SOCIAL LOGINS =================
    @Override
    public AuthenticationResponse socialLoginGoogle(SocialLoginRequest request) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(request.getToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null || !userInfo.containsKey("email")) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String picture = (String) userInfo.get("picture");

            User user = findOrCreateSocialUser(email, name, picture);
            String token = generateToken(user);
            return AuthenticationResponse.builder().token(token).authenticated(true).build();
        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    public AuthenticationResponse socialLoginFacebook(SocialLoginRequest request) {
        try {
            // Defensive fallback - env var might not load correctly
            String effectiveRedirectUri = (facebookRedirectUri != null && !facebookRedirectUri.isBlank())
                    ? facebookRedirectUri.trim()
                    : "http://localhost:3000/login/facebook/callback";

            log.info("Facebook login - AppId: [{}], RedirectUri: [{}], Code length: {}",
                    facebookAppId, effectiveRedirectUri,
                    request.getCode() != null ? request.getCode().length() : 0);

            // Step 1: Exchange authorization code for access_token
            // Use UriComponentsBuilder to avoid double-encoding issues
            URI tokenUri = UriComponentsBuilder
                    .fromHttpUrl("https://graph.facebook.com/v19.0/oauth/access_token")
                    .queryParam("client_id", facebookAppId)
                    .queryParam("redirect_uri", effectiveRedirectUri)
                    .queryParam("client_secret", facebookAppSecret)
                    .queryParam("code", request.getCode())
                    .build()
                    .toUri();

            log.info("Facebook token URI: {}", tokenUri.toString().replaceAll("client_secret=[^&]+", "client_secret=***"));
            ResponseEntity<Map> tokenResponse = restTemplate.getForEntity(tokenUri, Map.class);
            Map<String, Object> tokenData = tokenResponse.getBody();
            if (tokenData == null || !tokenData.containsKey("access_token")) {
                log.error("Facebook token exchange failed: {}", tokenData);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String accessToken = (String) tokenData.get("access_token");

            // Step 2: Get user info using the access_token
            String userInfoUrl = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token=" + accessToken;
            ResponseEntity<Map> response = restTemplate.getForEntity(userInfoUrl, Map.class);
            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String id = (String) userInfo.get("id");
            String email = id + "@facebook.com"; // Facebook rarely gives email without special approval
            String name = (String) userInfo.get("name");
            String picture = null;
            if (userInfo.containsKey("picture")) {
                Map<String, Object> picData = (Map<String, Object>) userInfo.get("picture");
                Map<String, Object> data = (Map<String, Object>) picData.get("data");
                if (data != null && data.containsKey("url")) {
                    picture = (String) data.get("url");
                }
            }

            User user = findOrCreateSocialUser(email, name, picture);
            String token = generateToken(user);
            return AuthenticationResponse.builder().token(token).authenticated(true).build();
        } catch (Exception e) {
            log.error("Facebook login failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    public AuthenticationResponse socialLoginZalo(SocialLoginRequest request) {
        // 1. Exchange code for access token
        String tokenUrl = "https://oauth.zaloapp.com/v4/oa/access_token"; // We use standard oauth endpoint for login: https://oauth.zaloapp.com/v4/access_token
        String actualTokenUrl = "https://oauth.zaloapp.com/v4/access_token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("secret_key", zaloAppSecret);
        
        String body = "app_id=" + zaloAppId + "&grant_type=authorization_code&code=" + request.getCode();
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Zalo exchange code: {}", request.getCode());
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(actualTokenUrl, entity, Map.class);
            Map<String, Object> tokenData = tokenResponse.getBody();
            log.info("Zalo token response: {}", tokenData);

            if (tokenData == null || !tokenData.containsKey("access_token")) {
                log.error("Zalo token exchange failed. Response: {}", tokenData);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String accessToken = (String) tokenData.get("access_token");

            // 2. Get user info
            String userInfoUrl = "https://graph.zalo.me/v2.0/me?fields=id,name,picture";
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("access_token", accessToken);
            HttpEntity<String> userEntity = new HttpEntity<>(userHeaders);

            ResponseEntity<Map> userResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userEntity, Map.class);
            Map<String, Object> userInfo = userResponse.getBody();
            log.info("Zalo user info response: {}", userInfo);

            if (userInfo == null || (userInfo.containsKey("error") && !userInfo.get("error").toString().equals("0"))) {
                log.error("Zalo get user info failed. Response: {}", userInfo);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String id = (String) userInfo.get("id");
            String name = (String) userInfo.get("name");
            String email = id + "@zalo.me"; // Zalo rarely returns email easily, so we fallback to id
            String picture = null;
            if (userInfo.containsKey("picture")) {
                Map<String, Object> picData = (Map<String, Object>) userInfo.get("picture");
                Map<String, Object> data = (Map<String, Object>) picData.get("data");
                if (data != null && data.containsKey("url")) {
                    picture = (String) data.get("url");
                }
            }

            User user = findOrCreateSocialUser(email, name, picture);
            String token = generateToken(user);
            return AuthenticationResponse.builder().token(token).authenticated(true).build();
        } catch (Exception e) {
            log.error("Zalo login failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private User findOrCreateSocialUser(String email, String name, String picture) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password for social users
                    .avatar(picture)
                    .active(true)
                    .roles(Collections.singleton(userRole))
                    .build();
            return userRepository.save(newUser);
        });
    }
}
