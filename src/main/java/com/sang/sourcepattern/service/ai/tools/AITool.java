package com.sang.sourcepattern.service.ai.tools;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Set;

public interface AITool {
    String getName();
    Set<String> getSupportedAgents();
    Map<String, Object> getSchema();
    ToolResult execute(Map<String, Object> args, Jwt jwt);
}
