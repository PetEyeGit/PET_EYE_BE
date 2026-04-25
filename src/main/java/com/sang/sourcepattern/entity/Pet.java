package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    User owner;

    String name;
    String species;
    String breed;
    float weight;
    LocalDate dob;
    String healthNote;
    
    @Builder.Default
    boolean isActive = true;
    String unactiveReason;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL)
    java.util.List<PetDocument> documents;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL)
    java.util.List<PetMedicalRecord> medicalRecords;
}
