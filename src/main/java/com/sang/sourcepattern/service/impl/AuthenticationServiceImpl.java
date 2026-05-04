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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
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
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        if (!user.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DEACTIVATED);
        }

<<<<<<< HEAD
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        
        if (!isAdmin) {
            boolean isShopOwner = user.getRoles().stream()
                    .anyMatch(role -> role.getName().equals("SHOP_OWNER"));

            if (isShopOwner) {
                boolean shopVerified = shopRepository.findByOwnerId(user.getId())
                        .map(shop -> shop.isVerified())
                        .orElse(false);
                if (!shopVerified) {
                    throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
                }
            }
        }
=======
        verifyShopOwnerStatus(user);
>>>>>>> 2bce3625c4c9432a8ab2789a1318ecb0783728e3

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .authenticated(true)
                .build();
    }

    // ================= GENERATE JWT =================
    private String generateToken(User user) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + VALID_DURATION * 1000);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getEmail())
                    .claim("email", user.getEmail())
                    .claim("roles", user.getRoles().stream().map(Role::getName).toList())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claimsSet);
            jwt.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwt.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Cannot generate token", e);
        }
    }

    // ================= INTROSPECT =================
    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws ParseException, JOSEException {
        SignedJWT jwt = SignedJWT.parse(request.getToken());

        if (!jwt.verify(new MACVerifier(SIGNER_KEY.getBytes()))) {
            return IntrospectResponse.builder().valid(false).build();
        }

        if (jwt.getJWTClaimsSet().getExpirationTime().before(new Date())) {
            return IntrospectResponse.builder().valid(false).build();
        }

        if (invalidatedTokenRepository.existsById(jwt.getJWTClaimsSet().getJWTID())) {
            return IntrospectResponse.builder().valid(false).build();
        }

        return IntrospectResponse.builder().valid(true).build();
    }

    // ================= LOGOUT =================
    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        SignedJWT jwt = SignedJWT.parse(request.getToken());
        String jti = jwt.getJWTClaimsSet().getJWTID();

        if (invalidatedTokenRepository.existsById(jti)) return;

        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .id(jti)
                .expiryTime(jwt.getJWTClaimsSet().getExpirationTime())
                .build());

        log.info("Logout success - token revoked: {}", jti);
    }

    // ================= REFRESH TOKEN =================
    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        SignedJWT jwt = SignedJWT.parse(request.getToken());
        String jti = jwt.getJWTClaimsSet().getJWTID();
        Date issuedAt = jwt.getJWTClaimsSet().getIssueTime();
        Date expiry = jwt.getJWTClaimsSet().getExpirationTime();

        if (invalidatedTokenRepository.existsById(jti)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (System.currentTimeMillis() > issuedAt.getTime() + REFRESHABLE_DURATION * 1000) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String email = jwt.getJWTClaimsSet().getStringClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        invalidatedTokenRepository.save(InvalidatedToken.builder().id(jti).expiryTime(expiry).build());

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .authenticated(true)
                .build();
    }

    // ================= SOCIAL LOGINS =================
    @Override
    public AuthenticationResponse socialLoginGoogle(SocialLoginRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(request.getToken());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Map<String, Object> userInfo = response.getBody();
            if (userInfo == null || !userInfo.containsKey("email")) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            User user = findOrCreateSocialUser(
                    (String) userInfo.get("sub"),
                    "google",
                    (String) userInfo.get("email"),
                    (String) userInfo.get("name"),
                    (String) userInfo.get("picture"));

            verifyShopOwnerStatus(user);

            return AuthenticationResponse.builder().token(generateToken(user)).authenticated(true).build();
        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    public AuthenticationResponse socialLoginFacebook(SocialLoginRequest request) {
        try {
            String effectiveRedirectUri = (facebookRedirectUri != null && !facebookRedirectUri.isBlank())
                    ? facebookRedirectUri.trim()
                    : "http://localhost:3000/login/facebook/callback";

            // Facebook requires POST with form-urlencoded body (NOT GET with query params)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", facebookAppId);
            params.add("client_secret", facebookAppSecret);
            params.add("redirect_uri", effectiveRedirectUri);
            params.add("code", request.getCode());

            Map<String, Object> tokenData = restTemplate.postForObject(
                    "https://graph.facebook.com/v19.0/oauth/access_token",
                    new HttpEntity<>(params, headers),
                    Map.class);

            if (tokenData == null || !tokenData.containsKey("access_token")) {
                log.error("Facebook token exchange failed: {}", tokenData);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String accessToken = (String) tokenData.get("access_token");
            Map<String, Object> userInfo = restTemplate.getForEntity(
                    "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token=" + accessToken,
                    Map.class).getBody();

            if (userInfo == null) throw new AppException(ErrorCode.UNAUTHENTICATED);

            String id = (String) userInfo.get("id");
            String email = (String) userInfo.get("email");
            boolean requiresEmailUpdate = false;
            if (email == null || email.isBlank()) {
                email = id + "@facebook.com";
                requiresEmailUpdate = true;
            }

            String picture = null;
            if (userInfo.containsKey("picture")) {
                Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) userInfo.get("picture")).get("data");
                if (data != null) picture = (String) data.get("url");
            }

            User user = findOrCreateSocialUser(id, "facebook", email, (String) userInfo.get("name"), picture);
            verifyShopOwnerStatus(user);
            return AuthenticationResponse.builder()
                    .token(generateToken(user))
                    .authenticated(true)
                    .requiresEmailUpdate(requiresEmailUpdate)
                    .build();
        } catch (Exception e) {
            log.error("Facebook login failed", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    @Override
    public AuthenticationResponse socialLoginZalo(SocialLoginRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("secret_key", zaloAppSecret);

        // Use MultiValueMap for proper form encoding
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("app_id", zaloAppId);
        body.add("grant_type", "authorization_code");
        body.add("code", request.getCode());

        try {
            log.info("Exchanging Zalo code for token. AppId: {}, Code: {}", zaloAppId, request.getCode());
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth.zaloapp.com/v4/access_token", // Corrected: removed /oa/ segment for social login
                    new HttpEntity<>(body, headers), Map.class);
            
            Map<String, Object> tokenData = response.getBody();
            log.info("Zalo token response status: {}", response.getStatusCode());

            if (tokenData == null || !tokenData.containsKey("access_token")) {
                log.error("Zalo token exchange failed. Response body: {}", tokenData);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String accessToken = (String) tokenData.get("access_token");
            log.info("Zalo access token obtained successfully");

            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.set("access_token", accessToken);

            Map<String, Object> userInfo = restTemplate.exchange(
                    "https://graph.zalo.me/v2.0/me?fields=id,name,picture,email",
                    HttpMethod.GET, new HttpEntity<>(userHeaders), Map.class).getBody();

            log.info("Zalo user info response: {}", userInfo);

            if (userInfo == null || (userInfo.containsKey("error") && !userInfo.get("error").toString().equals("0"))) {
                log.error("Zalo get user info failed. Response: {}", userInfo);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String id = (String) userInfo.get("id");
            String email = (String) userInfo.get("email");
            boolean requiresEmailUpdate = false;
            if (email == null || email.isBlank()) {
                email = id + "@zalo.me";
                requiresEmailUpdate = true;
            }

            String picture = null;
            if (userInfo.containsKey("picture")) {
                Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) userInfo.get("picture")).get("data");
                if (data != null) picture = (String) data.get("url");
            }

            User user = findOrCreateSocialUser(id, "zalo", email, (String) userInfo.get("name"), picture);
            
            // Double check existence in DB
            if (!userRepository.existsById(user.getId())) {
                log.error("CRITICAL: User ID {} was returned but is NOT found in DB!", user.getId());
            } else {
                log.info("User ID {} successfully verified in DB before token generation", user.getId());
            }

            verifyShopOwnerStatus(user);
            return AuthenticationResponse.builder()
                    .token(generateToken(user))
                    .authenticated(true)
                    .requiresEmailUpdate(requiresEmailUpdate)
                    .build();
        } catch (Exception e) {
            log.error("Zalo login exception: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private void verifyShopOwnerStatus(User user) {
        boolean isShopOwner = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("SHOP_OWNER"));

        if (isShopOwner) {
            boolean shopVerified = shopRepository.findByOwnerId(user.getId())
                    .map(shop -> shop.isVerified())
                    .orElse(false);
            if (!shopVerified) {
                throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
            }
        }
    }

    private User findOrCreateSocialUser(String socialId, String provider, String email, String name, String picture) {
        java.util.Optional<User> userOpt = java.util.Optional.empty();
        if ("zalo".equals(provider)) {
            userOpt = userRepository.findByZaloId(socialId);
        } else if ("facebook".equals(provider)) {
            userOpt = userRepository.findByFacebookId(socialId);
        } else if ("google".equals(provider)) {
            userOpt = userRepository.findByGoogleId(socialId);
        }

        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // Not found by Social ID, check by Email
        return userRepository.findByEmail(email).map(user -> {
            // Link social account to existing user
            if ("zalo".equals(provider)) user.setZaloId(socialId);
            else if ("facebook".equals(provider)) user.setFacebookId(socialId);
            else if ("google".equals(provider)) user.setGoogleId(socialId);
            return userRepository.saveAndFlush(user);
        }).orElseGet(() -> {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .avatar(picture)
                    .active(true)
                    .emailVerified(true)
                    .roles(Collections.singleton(userRole))
                    .build();
            if ("zalo".equals(provider)) newUser.setZaloId(socialId);
            else if ("facebook".equals(provider)) newUser.setFacebookId(socialId);
            else if ("google".equals(provider)) newUser.setGoogleId(socialId);
            
            try {
                User savedUser = userRepository.saveAndFlush(newUser);
                log.info("Successfully created and persisted user: {} with ID: {}", email, savedUser.getId());
                return savedUser;
            } catch (Exception e) {
                log.error("Failed to persist new social user: " + email, e);
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }
        });
    }
    @Override
    public AuthenticationResponse updateEmail(UpdateEmailRequest request) {
        var context = SecurityContextHolder.getContext();
        String identifier = context.getAuthentication().getName();

        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        user.setEmail(request.getEmail());
        userRepository.saveAndFlush(user);

        return AuthenticationResponse.builder()
                .token(generateToken(user))
                .authenticated(true)
                .requiresEmailUpdate(false)
                .build();
    }
}
