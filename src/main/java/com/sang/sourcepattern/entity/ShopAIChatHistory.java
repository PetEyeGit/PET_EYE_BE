package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_ai_chat_history",
       indexes = { @Index(name = "idx_shop_ai_shop_id", columnList = "shop_id") })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopAIChatHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "shop_id", nullable = false)
    Integer shopId;

    @Column(nullable = false, length = 20)
    String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt = LocalDateTime.now();
}