package com.sang.sourcepattern.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @PostConstruct
    public void logConfig() {
        log.info("=== PayOS Config LOADED ===");
        log.info("CLIENT_ID    : [{}]", clientId);
        log.info("API_KEY      : [{}]", apiKey);
        log.info("CHECKSUM_KEY : [{}]", checksumKey);
        log.info("KEY_LENGTH   : {}", checksumKey != null ? checksumKey.length() : 0);
        log.info("===========================");
    }
}
