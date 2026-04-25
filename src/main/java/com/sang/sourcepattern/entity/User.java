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

    String password;
    String fullName;
    String phone;
    String address;
    String avatar;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany
    @Builder.Default
    Set<Role> roles = new HashSet<>();
}
