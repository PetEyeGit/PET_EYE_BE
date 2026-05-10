package com.sang.sourcepattern.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI Gateway configuration.
 * RestTemplate bean is provided by RestTemplateConfig (configuration package).
 * ObjectMapper bean is auto-configured by Spring Boot.
 * EnableAsync allows fire-and-forget history saving.
 */
@Configuration
@EnableAsync
public class AIGatewayConfig {
}
