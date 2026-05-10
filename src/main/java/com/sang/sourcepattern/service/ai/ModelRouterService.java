package com.sang.sourcepattern.service.ai;

import com.sang.sourcepattern.service.ai.provider.AIProvider;
import com.sang.sourcepattern.service.ai.provider.AIRequest;
import com.sang.sourcepattern.service.ai.provider.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelRouterService {

    private final List<AIProvider> providers;

    /**
     * Route request to the preferred provider, fallback to others on failure.
     */
    public AIResponse route(AIRequest request) {
        String preferred = request.getPreferredProvider() != null
                ? request.getPreferredProvider()
                : "gemini";

        // Try preferred provider first
        for (AIProvider provider : providers) {
            if (provider.getName().equals(preferred) && provider.isAvailable()) {
                try {
                    AIResponse response = provider.generate(request);
                    log.info("[ModelRouter] Success with provider={} model={}", preferred, response.getModelUsed());
                    return response;
                } catch (Exception e) {
                    log.warn("[ModelRouter] Primary provider {} failed: {}", preferred, e.getMessage());
                }
            }
        }

        // Fallback to other providers
        for (AIProvider provider : providers) {
            if (!provider.getName().equals(preferred) && provider.isAvailable()) {
                try {
                    log.info("[ModelRouter] Falling back to provider: {}", provider.getName());
                    AIResponse response = provider.generate(request);
                    return response;
                } catch (Exception e) {
                    log.warn("[ModelRouter] Fallback provider {} failed: {}", provider.getName(), e.getMessage());
                }
            }
        }

        throw new RuntimeException("All AI providers exhausted. Please check API keys and quota.");
    }
}
