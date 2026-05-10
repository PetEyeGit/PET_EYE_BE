package com.sang.sourcepattern.service.ai.provider;

public interface AIProvider {
    String getName();
    AIResponse generate(AIRequest request);
    boolean isAvailable();
}
