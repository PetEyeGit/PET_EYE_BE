package com.sang.sourcepattern.service.ai;

import com.sang.sourcepattern.dto.request.ai.AIChatRequest;
import com.sang.sourcepattern.dto.response.ai.AIChatResponse;
import com.sang.sourcepattern.dto.response.ai.AIHistoryItem;
import com.sang.sourcepattern.entity.ai.AIMessage;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.service.ai.provider.AIRequest;
import com.sang.sourcepattern.service.ai.provider.AIResponse;
import com.sang.sourcepattern.service.ai.tools.ToolExecutorService;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestrationService {

    private final PromptBuilderService promptBuilder;
    private final ConversationMemoryService memoryService;
    private final ModelRouterService modelRouter;
    private final ToolExecutorService toolExecutor;
    private final SafetyFilterService safetyFilter;
    private final ContextBuilderService contextBuilder;

    /**
     * Main orchestration flow:
     * 1. Safety check
     * 2. Validate agent access
     * 3. Load conversation memory
     * 4. Build business context
     * 5. Build prompt
     * 6. Call model
     * 7. Execute tools if needed (USER_CHAT only)
     * 8. Call model again with tool result
     * 9. Save to memory
     * 10. Return response
     */
    public AIChatResponse processMessage(AIChatRequest request, Jwt jwt) {
        String userEmail = jwt.getClaim("email");
        String agentType = request.getAgentType();

        log.info("[AI] Processing message agentType={} user={}", agentType, userEmail);

        // 1. Safety check
        safetyFilter.validate(request.getMessage());

        // 2. Load conversation memory
        List<Map<String, String>> history = memoryService.loadHistory(
                agentType, userEmail, request.getSessionId());

        // 3. Build business context
        String businessContext = contextBuilder.buildContext(agentType, jwt);

        // 4. Build prompt
        AIRequest aiRequest = promptBuilder.build(
                agentType, request.getMessage(), history, businessContext, jwt);

        // 5. Call model
        AIResponse aiResponse;
        try {
            aiResponse = modelRouter.route(aiRequest);
        } catch (Exception e) {
            log.error("[AI] Model call failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        // 6. Tool execution (USER_CHAT only)
        ToolResult toolResult = null;
        if (aiResponse.hasFunctionCall() && "USER_CHAT".equals(agentType)) {
            toolResult = toolExecutor.execute(aiResponse.getFunctionCall(), jwt);

            // 7. Build second request with tool result
            AIRequest requestWithTool = promptBuilder.buildWithToolResult(
                    aiRequest, aiResponse.getFunctionCall(), toolResult);

            // 8. Call model again
            try {
                aiResponse = modelRouter.route(requestWithTool);
            } catch (Exception e) {
                log.warn("[AI] Second model call failed, returning tool result only: {}", e.getMessage());
                aiResponse = com.sang.sourcepattern.service.ai.provider.AIResponse.builder()
                        .text("Đã tìm thấy kết quả. Xem bên dưới.")
                        .modelUsed("fallback")
                        .build();
            }
        }

        // 9. Save to memory (async-style: don't fail the response if save fails)
        String finalText = aiResponse.getText() != null ? aiResponse.getText() : "Đã xử lý xong.";
        try {
            memoryService.saveMessage(agentType, userEmail, request.getSessionId(),
                    request.getMessage(), finalText, toolResult);
        } catch (Exception e) {
            log.warn("[AI] Failed to save message to memory: {}", e.getMessage());
        }

        log.info("[AI] Done agentType={} model={} tokens={}", agentType,
                aiResponse.getModelUsed(), aiResponse.getTotalTokens());

        // 10. Return
        String sessionId = memoryService.resolveSessionId(agentType, userEmail, request.getSessionId());
        return AIChatResponse.builder()
                .text(finalText)
                .toolResultJson(toolResult != null ? toolResult.toJson() : null)
                .sessionId(sessionId)
                .model(aiResponse.getModelUsed())
                .build();
    }

    public List<AIHistoryItem> getHistory(String agentType, Jwt jwt) {
        String userEmail = jwt.getClaim("email");
        List<AIMessage> messages = memoryService.loadRawHistory(agentType, userEmail);
        return messages.stream()
                .map(m -> AIHistoryItem.builder()
                        .id(m.getId())
                        .role(m.getRole())
                        .content(m.getContent())
                        .toolResultJson(m.getToolResultJson())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public void clearHistory(String agentType, Jwt jwt) {
        String userEmail = jwt.getClaim("email");
        memoryService.clearHistory(agentType, userEmail);
        log.info("[AI] History cleared agentType={} user={}", agentType, userEmail);
    }
}
