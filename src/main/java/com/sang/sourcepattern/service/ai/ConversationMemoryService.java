package com.sang.sourcepattern.service.ai;

import com.sang.sourcepattern.entity.ai.AIMessage;
import com.sang.sourcepattern.repository.ai.AIMessageRepository;
import com.sang.sourcepattern.service.ai.tools.ToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMemoryService {

    private final AIMessageRepository messageRepository;

    @Value("${ai.memory.max-turns:10}")
    private int maxTurns;

    @Value("${ai.memory.max-tokens:8000}")
    private int maxTokens;

    /**
     * Load conversation history as [{role, content}] maps for Gemini.
     * Applies sliding window (maxTurns pairs) and token budget.
     */
    public List<Map<String, String>> loadHistory(String agentType, String ownerKey, String sessionId) {
        String resolvedSession = resolveSessionId(agentType, ownerKey, sessionId);

        List<AIMessage> allMessages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(resolvedSession);

        // Sliding window: keep last maxTurns * 2 messages
        int startIdx = Math.max(0, allMessages.size() - (maxTurns * 2));
        List<AIMessage> window = allMessages.subList(startIdx, allMessages.size());

        // Apply token budget (estimate: 4 chars ≈ 1 token)
        List<AIMessage> budgeted = applyTokenBudget(window);

        return budgeted.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());    }

    /**
     * Load raw AIMessage entities for history endpoint.
     */
    public List<AIMessage> loadRawHistory(String agentType, String ownerKey) {
        return messageRepository.findByOwnerKeyAndAgentTypeOrderByCreatedAtAsc(ownerKey, agentType);
    }

    public void saveMessage(String agentType, String ownerKey, String sessionId,
                            String userMessage, String assistantResponse,
                            ToolResult toolResult) {
        String resolvedSession = resolveSessionId(agentType, ownerKey, sessionId);

        messageRepository.save(AIMessage.builder()
                .sessionId(resolvedSession)
                .agentType(agentType)
                .ownerKey(ownerKey)
                .role("user")
                .content(userMessage)
                .build());

        messageRepository.save(AIMessage.builder()
                .sessionId(resolvedSession)
                .agentType(agentType)
                .ownerKey(ownerKey)
                .role("assistant")
                .content(assistantResponse)
                .toolResultJson(toolResult != null ? toolResult.toJson() : null)
                .build());
    }

    public void clearHistory(String agentType, String ownerKey) {
        messageRepository.deleteByOwnerKeyAndAgentType(ownerKey, agentType);
    }

    /**
     * Session ID strategy:
     * USER_CHAT    → "user_chat_{email}"
     * SHOP_ASSISTANT → "shop_{email}"
     * ADMIN_ASSISTANT → "admin_{email}"
     */
    public String resolveSessionId(String agentType, String ownerKey, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) return sessionId;
        return agentType.toLowerCase().replace("_", "-") + "_" + ownerKey;
    }

    private List<AIMessage> applyTokenBudget(List<AIMessage> messages) {
        int totalChars = 0;
        List<AIMessage> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            int chars = messages.get(i).getContent().length();
            if (totalChars + chars > maxTokens * 4) break;
            totalChars += chars;
            result.add(0, messages.get(i));
        }
        return result;
    }
}
