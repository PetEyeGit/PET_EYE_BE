package com.sang.sourcepattern.service.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GeminiProvider implements AIProvider {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models";

    private static final List<String> MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-flash-latest",
            "gemini-2.0-flash"
    );

    @Value("${ai.gemini.keys:}")
    private String apiKeysString;

    private List<String> apiKeys;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    public GeminiProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Autowired
    private void initApiKeys() {
        if (apiKeysString != null && !apiKeysString.isBlank()) {
            // Split comma-separated keys
            apiKeys = Arrays.stream(apiKeysString.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            apiKeys = Collections.emptyList();
        }
        log.info("[Gemini] Initialized with {} API keys", apiKeys.size());
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        return apiKeys != null && !apiKeys.isEmpty();
    }

    @Override
    public AIResponse generate(AIRequest request) {
        if (!isAvailable()) {
            throw new RuntimeException("No Gemini API keys configured");
        }

        List<String> errors = new ArrayList<>();

        for (String model : MODELS) {
            // Try each key for this model
            for (int attempt = 0; attempt < apiKeys.size(); attempt++) {
                String key = getNextKey();
                try {
                    log.debug("[Gemini] Trying model={} key={}...", model, key.substring(0, Math.min(8, key.length())));
                    return callGemini(model, key, request);
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        log.warn("[Gemini] 429 on model={} key={}..., rotating key", model, key.substring(0, 8));
                        errors.add(model + "/key" + attempt + ": 429");
                        // continue to next key
                    } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        log.error("[Gemini] 400 Bad Request on model={}: {}", model, e.getMessage());
                        throw new RuntimeException("Gemini bad request: " + e.getMessage());
                    } else {
                        log.error("[Gemini] HTTP {} on model={}: {}", e.getStatusCode(), model, e.getMessage());
                        throw new RuntimeException("Gemini error " + e.getStatusCode() + ": " + e.getMessage());
                    }
                } catch (HttpServerErrorException e) {
                    if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                        log.warn("[Gemini] 503 on model={}, trying next model", model);
                        errors.add(model + ": 503");
                        break; // try next model
                    }
                    log.error("[Gemini] Server error on model={}: {}", model, e.getMessage());
                    errors.add(model + ": " + e.getStatusCode());
                    break;
                } catch (Exception e) {
                    log.error("[Gemini] Unexpected error on model={}: {}", model, e.getMessage());
                    errors.add(model + ": " + e.getMessage());
                    break;
                }
            }
        }

        throw new RuntimeException("All Gemini models exhausted. Errors: " + String.join(", ", errors));
    }

    @SuppressWarnings("unchecked")
    private AIResponse callGemini(String model, String key, AIRequest request) {
        String url = BASE_URL + "/" + model + ":generateContent?key=" + key;

        Map<String, Object> body = buildRequestBody(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        Map<?, ?> responseBody = response.getBody();

        if (responseBody == null) {
            throw new RuntimeException("Empty response from Gemini");
        }

        return parseResponse(responseBody, model);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestBody(AIRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        // System instruction
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            body.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", request.getSystemPrompt()))
            ));
        }

        // Conversation history
        List<Map<String, Object>> contents = request.getMessages().stream()
                .map(msg -> {
                    String geminiRole = "user".equals(msg.get("role")) ? "user" : "model";
                    return (Map<String, Object>) new LinkedHashMap<>(Map.of(
                            "role", geminiRole,
                            "parts", List.of(Map.of("text", msg.get("content")))
                    ));
                })
                .collect(Collectors.toList());
        body.put("contents", contents);

        // Tools (function calling)
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", List.of(
                    Map.of("function_declarations", request.getTools())
            ));
        }

        // Generation config
        body.put("generationConfig", Map.of(
                "temperature", request.getTemperature(),
                "maxOutputTokens", request.getMaxOutputTokens()
        ));

        return body;
    }

    @SuppressWarnings("unchecked")
    private AIResponse parseResponse(Map<?, ?> body, String model) {
        try {
            List<?> candidates = (List<?>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return AIResponse.builder()
                        .text("Không có phản hồi từ AI.")
                        .modelUsed(model)
                        .build();
            }

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);

            // Token usage
            int promptTokens = 0;
            int outputTokens = 0;
            Map<?, ?> usageMeta = (Map<?, ?>) body.get("usageMetadata");
            if (usageMeta != null) {
                Object pt = usageMeta.get("promptTokenCount");
                Object ot = usageMeta.get("candidatesTokenCount");
                if (pt instanceof Number) promptTokens = ((Number) pt).intValue();
                if (ot instanceof Number) outputTokens = ((Number) ot).intValue();
            }

            // Function call?
            if (part.containsKey("functionCall")) {
                Map<?, ?> fc = (Map<?, ?>) part.get("functionCall");
                String name = (String) fc.get("name");
                Map<String, Object> args = (Map<String, Object>) fc.get("args");
                return AIResponse.builder()
                        .functionCall(FunctionCall.builder().name(name).args(args).build())
                        .modelUsed(model)
                        .promptTokens(promptTokens)
                        .outputTokens(outputTokens)
                        .build();
            }

            // Text response
            String text = (String) part.get("text");
            return AIResponse.builder()
                    .text(text != null ? text : "Không có phản hồi.")
                    .modelUsed(model)
                    .promptTokens(promptTokens)
                    .outputTokens(outputTokens)
                    .build();

        } catch (Exception e) {
            log.error("[Gemini] Failed to parse response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    private String getNextKey() {
        int idx = Math.abs(keyIndex.getAndIncrement() % apiKeys.size());
        return apiKeys.get(idx);
    }
}
