package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    Staff staff;

    String certificateName;
    
    @Column(columnDefinition = "TEXT")
    String imageUrl;
    
    LocalDate issueDate;
    LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    CertificateStatus status = CertificateStatus.PENDING;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    public enum CertificateStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
