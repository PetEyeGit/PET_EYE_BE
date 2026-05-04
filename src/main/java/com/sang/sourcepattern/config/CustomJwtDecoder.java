package com.sang.sourcepattern.config;

import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signer-key}")
    private String signerKey;

    private final UserRepository userRepository;
    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        if (token.startsWith("ya29.")) {
            throw new JwtException("Google access token - skip local JWT decode");
        }

        try {
            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HmacSHA512");
                nimbusJwtDecoder = NimbusJwtDecoder
                        .withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
            }

            Jwt jwt = nimbusJwtDecoder.decode(token);
            
            // Security check: Verify if user account is still active in database
            String identifier = jwt.getSubject();
            if (identifier != null) {
                Optional<User> userOpt;
                if (identifier.contains("@")) {
                    userOpt = userRepository.findByEmail(identifier);
                } else {
                    try {
                        userOpt = userRepository.findById(Integer.parseInt(identifier));
                    } catch (NumberFormatException e) {
                        userOpt = Optional.empty();
                    }
                }

                if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                    System.out.println(">>> JWT Debug: User not found or inactive for identifier: " + identifier);
                    throw new JwtException("User account is deactivated or not found");
                }
            }

            return jwt;

        } catch (Exception e) {
            if (e instanceof JwtException) throw (JwtException) e;
            throw new JwtException("JWT decode failed: " + e.getMessage());
        }
    }
}
