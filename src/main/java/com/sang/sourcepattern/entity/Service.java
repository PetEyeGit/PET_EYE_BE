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

    @Builder.Default
    String petType = "DOG"; // DOG, CAT, OTHER

    BigDecimal price;
    int durationMinutes;

    @Column(columnDefinition = "TEXT")
    String description;

    String imageUrl;

    @Builder.Default
    boolean active = true;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    // ── BOARDING-only: camera configuration ──────────────────────────────────

    @Builder.Default
    boolean cameraEnabled = false;

    // ── BOARDING-only details ────────────────────────────────────────────────
    String petWeight;
    String roomType;
    @Column(columnDefinition = "TEXT")
    String roomTypePrices;

    String prices;

    /**
     * JSON array of supported camera tier IDs, e.g. ["BASIC","HD","AI"]
     */
    @Column(columnDefinition = "TEXT")
    String cameraTiers; // stored as JSON: ["BASIC","HD"]

    /**
     * JSON map of custom prices per tier (VND extra/day), e.g. {"BASIC":0,"HD":60000,"AI":200000}
     * If null/missing for a tier, falls back to default prices.
     */
    @Column(columnDefinition = "TEXT")
    String cameraTierPrices; // stored as JSON: {"BASIC":0,"HD":60000}

    /**
     * JSON map of custom display labels per tier, e.g. {"BASIC":"Tiêu chuẩn","HD":"Nét cao"}
     * If null/missing for a tier, falls back to default labels.
     */
    @Column(columnDefinition = "TEXT")
    String cameraTierLabels; // stored as JSON: {"BASIC":"Tiêu chuẩn"}

    /** Camera description shown to user (replaces general description in camera section) */
    String cameraDescription;
}
