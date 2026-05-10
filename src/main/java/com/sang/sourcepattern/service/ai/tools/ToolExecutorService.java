package com.sang.sourcepattern.service.ai.tools;

import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.service.ai.provider.FunctionCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolExecutorService {

    private final ToolRegistry toolRegistry;

    public ToolResult execute(FunctionCall functionCall, Jwt jwt) {
        String toolName = functionCall.getName();
        log.info("[ToolExecutor] Executing tool: {} args keys: {}", toolName,
                functionCall.getArgs() != null ? functionCall.getArgs().keySet() : "none");

        try {
            AITool tool = toolRegistry.findTool(toolName);
            ToolResult result = tool.execute(functionCall.getArgs(), jwt);
            log.info("[ToolExecutor] Tool {} completed, type: {}", toolName, result.getType());
            return result;
        } catch (AppException e) {
            log.warn("[ToolExecutor] Tool {} AppException: {}", toolName, e.getMessage());
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("[ToolExecutor] Tool {} unexpected error: {}", toolName, e.getMessage(), e);
            return ToolResult.error("Lỗi thực thi: " + e.getMessage());
        }
    }
}
