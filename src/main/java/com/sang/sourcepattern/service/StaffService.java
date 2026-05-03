package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.StaffCreationRequest;
import com.sang.sourcepattern.dto.request.StaffUpdateRequest;
import com.sang.sourcepattern.dto.response.StaffResponse;

import java.util.List;

public interface StaffService {

    /**
     * Shop Owner creates a new staff account.
     * Internally creates a User with STAFF role, then creates a Staff record
     * linked to the owner's shop.
     */
    StaffResponse createStaff(StaffCreationRequest request, String ownerEmail);

    /**
     * Get all staff members of the owner's shop.
     */
    List<StaffResponse> getMyShopStaff(String ownerEmail);

    /**
     * Get a single staff member by ID.
     */
    StaffResponse getStaffById(int staffId, String ownerEmail);

    /**
     * Get the logged-in staff member's own profile.
     */
    StaffResponse getMyProfile(String email);

    /**
     * Toggle active/inactive status of a staff member.
     */
    StaffResponse toggleStaffStatus(int staffId, String ownerEmail);

    /**
     * Update staff details.
     */
    StaffResponse updateStaff(int staffId, StaffUpdateRequest request, String ownerEmail);

    // ─── Certificates ──────────────────────────────────────────────────────────
    
    /** Add a certificate to a staff member */
    StaffResponse addCertificate(int staffId, com.sang.sourcepattern.dto.request.StaffCertificateRequest request, String ownerEmail);

    /** Remove a certificate */
    void removeCertificate(int certId, String ownerEmail);

    /** Verify/Reject a certificate (Owner only) */
    StaffResponse verifyCertificate(int certId, com.sang.sourcepattern.entity.StaffCertificate.CertificateStatus status, String ownerEmail);
}
