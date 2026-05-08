package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CareLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    Staff staff;

    String type; // FEEDING, CLEANING, MEDICAL, EXERCISE, etc.
    
    @Column(columnDefinition = "TEXT")
    String note;

    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();
    
    String imageUrl; // Optional: photo of the activity
}
