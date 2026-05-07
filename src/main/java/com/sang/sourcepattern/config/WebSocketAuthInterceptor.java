package com.sang.sourcepattern.config;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Value("${jwt.signer-key}")
    private String signerKey;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    SignedJWT jwt = SignedJWT.parse(token);
                    jwt.verify(new MACVerifier(signerKey.getBytes()));

                    String email = jwt.getJWTClaimsSet().getStringClaim("email");
                    List<String> roles = jwt.getJWTClaimsSet().getStringListClaim("roles");

                    // Set principal để dùng trong @MessageMapping
                    accessor.setUser(new WsPrincipal(email, roles));
                    log.info("WebSocket authenticated: {} with roles: {}", email, roles);
                } catch (Exception e) {
                    log.warn("WebSocket auth failed: {}", e.getMessage());
                }
            }
        }
        return message;
    }

    /** Simple Principal wrapper mang email + roles */
    public record WsPrincipal(String email, List<String> roles) implements Principal {
        @Override
        public String getName() {
            return email;
        }
    }
}
