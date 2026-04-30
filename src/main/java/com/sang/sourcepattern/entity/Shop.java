package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    User owner;

    String shopName;
    String shopType;
    String email;
    String phone;
    String address;
    String city;
    String description;
    String licenseNumber;
    String licenseImageUrl;

    String openTime;
    String closeTime;
    String workingDays;

    @Builder.Default
    float ratingAvg = 0.0f;

    @Builder.Default
    boolean isVerified = false;

    @OneToMany(mappedBy = "shop")
    List<Service> services;

    @OneToMany(mappedBy = "shop")
    List<Staff> staffs;

    @OneToMany(mappedBy = "shop")
    List<Cage> cages;
}
