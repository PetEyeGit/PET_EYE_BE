package com.sang.sourcepattern.controller.ai;

import com.sang.sourcepattern.dto.request.ai.AIChatRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ai.AIChatResponse;
import com.sang.sourcepattern.dto.response.ai.AIHistoryItem;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.service.ai.AIOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Gateway", description = "Unified AI endpoint — User/Shop/Admin AI")
public class AIController {

    private final AIOrchestrationService orchestrationService;

    /**
     * Single endpoint for all 3 AI agents.
     * agentType: USER_CHAT | SHOP_ASSISTANT | ADMIN_ASSISTANT
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Send a message to the AI agent")
    public ApiResponse<AIChatResponse> chat(
            @RequestBody @Valid AIChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        validateAgentAccess(request.getAgentType(), jwt);
        AIChatResponse response = orchestrationService.processMessage(request, jwt);
        return ApiResponse.<AIChatResponse>builder().result(response).build();
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get conversation history for an agent")
    public ApiResponse<List<AIHistoryItem>> getHistory(
            @RequestParam String agentType,
            @AuthenticationPrincipal Jwt jwt) {

        validateAgentAccess(agentType, jwt);
        return ApiResponse.<List<AIHistoryItem>>builder()
                .result(orchestrationService.getHistory(agentType, jwt))
                .build();
    }

    @DeleteMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear conversation history for an agent")
    public ApiResponse<Void> clearHistory(
            @RequestParam String agentType,
            @AuthenticationPrincipal Jwt jwt) {

        validateAgentAccess(agentType, jwt);
        orchestrationService.clearHistory(agentType, jwt);
        return ApiResponse.<Void>builder().message("Đã xóa lịch sử chat AI").build();
    }

    // ── Access control ────────────────────────────────────────────────────────

    private static final Set<String> VALID_AGENT_TYPES =
            Set.of("USER_CHAT", "SHOP_ASSISTANT", "ADMIN_ASSISTANT");

    private void validateAgentAccess(String agentType, Jwt jwt) {
        if (!VALID_AGENT_TYPES.contains(agentType)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) roles = List.of();

        switch (agentType) {
            case "SHOP_ASSISTANT" -> {
                if (!roles.contains("SHOP_OWNER"))
                    throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            case "ADMIN_ASSISTANT" -> {
                if (!roles.contains("ADMIN"))
                    throw new AppException(ErrorCode.UNAUTHORIZED);
            }
            case "USER_CHAT" -> {
                if (!roles.contains("USER"))
                    throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }
    }
}
