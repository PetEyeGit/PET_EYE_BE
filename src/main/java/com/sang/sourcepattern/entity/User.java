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

    @Builder.Default
    boolean active = true;

    @Builder.Default
    boolean emailVerified = false;

    @ManyToMany
    @Builder.Default
    Set<Role> roles = new HashSet<>();
}
