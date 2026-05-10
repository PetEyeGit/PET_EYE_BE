package com.sang.sourcepattern.service.ai.tools;

import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final List<AITool> tools;

    /** Get JSON schemas for all tools that support the given agentType */
    public List<Map<String, Object>> getSchemas(String agentType) {
        return tools.stream()
                .filter(t -> t.getSupportedAgents().contains(agentType))
                .map(AITool::getSchema)
                .collect(Collectors.toList());
    }

    public AITool findTool(String name) {
        return tools.stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
    }
}
