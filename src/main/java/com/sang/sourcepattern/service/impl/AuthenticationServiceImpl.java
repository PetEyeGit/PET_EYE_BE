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
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.AuthenticationService;
import lombok.AccessLevel;
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
    PasswordEncoder passwordEncoder;
    InvalidatedTokenRepository invalidatedTokenRepository;

    @Value("${jwt.signer-key}")
    @NonFinal
    String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    @NonFinal
    long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    @NonFinal
    long REFRESHABLE_DURATION;

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
}
