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
    Integer id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    User owner;

    String name;
    String species;
    String breed;
    String gender;
    String color;
    String avatar;
    boolean sterilized;
    float weight;
    LocalDate dob;
    String healthNote;
    String favoriteFood;
    String allergies;
    String hobbies;
    String walkTime;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    java.util.List<PetMeal> nutritionPlan;
    
    @Builder.Default
    @Column(name = "is_active")
    boolean active = true;
    String unactiveReason;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    java.util.List<PetDocument> documents;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    java.util.List<PetMedicalRecord> medicalRecords;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    java.util.List<PetVaccination> vaccinations;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    java.util.List<PetReminder> reminders;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    java.util.List<PetImage> album;
}
