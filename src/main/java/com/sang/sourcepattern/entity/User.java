package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(unique = true)
    String email;

    @Column(unique = true)
    String facebookId;
    @Column(unique = true)
    String zaloId;
    @Column(unique = true)
    String googleId;

    String password;
    String fullName;
    String phone;
    String address;
    @Column(columnDefinition = "TEXT")
    String avatar;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    boolean active;

    @Builder.Default
    boolean emailVerified = false;

    @Builder.Default
    Boolean justUpgraded = false;

    @Builder.Default
    Boolean justRegistered = false;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    MembershipTier currentTier;

    @Builder.Default
    Double totalSpending = 0.0;

    @Builder.Default
    @Column(name = "failed_login_attempts")
    Integer failedLoginAttempts = 0;

    @ManyToMany
    @Builder.Default
    Set<Role> roles = new HashSet<>();
}
