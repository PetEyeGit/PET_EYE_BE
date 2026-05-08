package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    /**
     * Lưu userId trực tiếp thay vì @ManyToOne để tránh Hibernate
     * generate JOIN vào bảng `user` (reserved keyword trong MySQL).
     */
    @Column(name = "user_id", nullable = false)
    Integer userId;

    /** "user" hoặc "assistant" */
    @Column(nullable = false, length = 20)
    String role;

    /** Nội dung tin nhắn */
    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    /** Tool result JSON (tuỳ chọn, lưu để hiển thị lại card) */
    @Column(name = "tool_result_json", columnDefinition = "TEXT")
    String toolResultJson;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();
}
