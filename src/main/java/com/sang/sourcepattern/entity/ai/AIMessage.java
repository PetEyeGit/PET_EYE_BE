package com.sang.sourcepattern.entity.ai;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Unified AI message store for all 3 agents.
 * Replaces ChatHistory, ShopAIChatHistory, AdminAIChatHistory for new gateway flow.
 * Legacy tables are kept for backward compatibility.
 */
@Entity
@Table(name = "ai_gateway_messages",
        indexes = {
                @Index(name = "idx_ai_msg_session", columnList = "session_id,created_at"),
                @Index(name = "idx_ai_msg_owner", columnList = "owner_key,agent_type")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Unique session identifier, e.g. "user_chat_user@email.com" */
    @Column(name = "session_id", nullable = false, length = 255)
    String sessionId;

    /** USER_CHAT | SHOP_ASSISTANT | ADMIN_ASSISTANT */
    @Column(name = "agent_type", nullable = false, length = 50)
    String agentType;

    /** Email of the authenticated user who owns this message */
    @Column(name = "owner_key", nullable = false, length = 255)
    String ownerKey;

    /** "user" or "assistant" */
    @Column(nullable = false, length = 20)
    String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    /** JSON of ToolResult — only for USER_CHAT assistant messages */
    @Column(name = "tool_result_json", columnDefinition = "TEXT")
    String toolResultJson;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();
}
