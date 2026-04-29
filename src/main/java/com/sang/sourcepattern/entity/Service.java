package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet_service")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    Shop shop;

    String serviceName;
    String category; // CLINIC, SPA, BOARDING, GROOMING, etc.
    BigDecimal price;
    int durationMinutes;

    @Column(columnDefinition = "TEXT")
    String description;

    String imageUrl;

    @Builder.Default
    boolean active = true;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}
