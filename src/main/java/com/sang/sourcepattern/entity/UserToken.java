package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Unified token entity dùng cho:
 * - VERIFY_EMAIL: xác thực email khi đăng ký (hết hạn 10 phút)
 * - RESET_PASSWORD: đặt lại mật khẩu (hết hạn 15 phút)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(unique = true, nullable = false)
    String token;


    @Column(nullable = false)
    String type;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    LocalDateTime expiresAt;

    @Builder.Default
    boolean used = false;
}
