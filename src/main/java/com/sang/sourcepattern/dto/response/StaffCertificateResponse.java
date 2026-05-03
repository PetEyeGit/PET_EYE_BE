package com.sang.sourcepattern.dto.response;

import com.sang.sourcepattern.entity.StaffCertificate.CertificateStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffCertificateResponse {
    int id;
    String certificateName;
    String imageUrl;
    LocalDate issueDate;
    LocalDate expiryDate;
    CertificateStatus status;
}
