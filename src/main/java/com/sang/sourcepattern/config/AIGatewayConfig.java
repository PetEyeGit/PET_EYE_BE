package com.sang.sourcepattern.config;

import com.sang.sourcepattern.service.ai.provider.AIProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

/**
 * AI Gateway configuration.
 * RestTemplate bean is provided by RestTemplateConfig (configuration package).
 * ObjectMapper bean is auto-configured by Spring Boot.
 * EnableAsync allows fire-and-forget history saving.
 */
@Configuration
@EnableAsync
public class AIGatewayConfig {
    
    /**
     * Collect all AIProvider implementations and provide as a List bean
     * for ModelRouterService to inject and route requests.
     */
    @Bean
    public List<AIProvider> aiProviders(List<AIProvider> providers) {
        return providers;
    }
}
