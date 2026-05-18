package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_change_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "old_staff_id")
    Staff oldStaff;

    @ManyToOne
    @JoinColumn(name = "proposed_staff_id")
    Staff proposedStaff;

    @Column(columnDefinition = "TEXT", nullable = false)
    String reason;

    @Builder.Default
    String status = "PENDING"; // PENDING, ACCEPTED, REJECTED

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    LocalDateTime processedAt;
}
