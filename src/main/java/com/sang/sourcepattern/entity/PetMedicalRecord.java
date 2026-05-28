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
public class PetMedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    Pet pet;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    Booking booking;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    Staff staff;

    String diagnosis;
    String treatment;
    String prescription;
    LocalDateTime visitDate;
    String veterinarianNote;
}
