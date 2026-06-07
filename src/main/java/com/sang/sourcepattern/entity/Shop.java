package com.sang.sourcepattern.entity;

import com.sang.sourcepattern.enums.ShopStatus;
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
    
    /** Tọa độ địa lý — lấy từ Goong Geocoding API */
    Double latitude;
    Double longitude;
    String description;
    String licenseNumber;
    String licenseImageUrl;
    String logoUrl;
    String bannerUrl;
    String galleryUrls;

    String openTime;
    String closeTime;
    String workingDays;
    
    @Column(columnDefinition = "TEXT")
    String offDays; // Comma-separated dates e.g. 2026-06-10,2026-06-11

    @Builder.Default
    float ratingAvg = 0.0f;

    /** Giữ lại để tương thích — true khi status = APPROVED */
    @Builder.Default
    boolean isVerified = false;

    /** PENDING | APPROVED | REJECTED */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    ShopStatus status = ShopStatus.PENDING;

    /** MANUAL | OPEN_POOL | AUTO — default: MANUAL */
    @Builder.Default
    String assignmentMode = "MANUAL";

    /** Số phút châm chước trước khi Staff được phép hủy đơn do khách không đến (No-Show). Min 5, Max 30. */
    @Builder.Default
    int lateGracePeriod = 15;

    @OneToMany(mappedBy = "shop")
    List<Service> services;

    @OneToMany(mappedBy = "shop")
    List<Staff> staffs;

    @OneToMany(mappedBy = "shop")
    List<Cage> cages;
}
