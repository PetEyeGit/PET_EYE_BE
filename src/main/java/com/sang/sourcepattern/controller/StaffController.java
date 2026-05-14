package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.StaffCreationRequest;
import com.sang.sourcepattern.dto.request.StaffUpdateRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Staff Management", description = "APIs for Shop Owner to manage staff accounts")
public class StaffController {

    StaffService staffService;

    /**
     * POST /staff — Create a new staff account (Owner only).
     * Body: { fullName, email, password, phone, role, specialization }
     */
    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Create a new staff account for the owner's shop")
    public ApiResponse<StaffResponse> createStaff(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid StaffCreationRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.createStaff(request, jwt.getClaim("email")))
                .message("Staff created successfully")
                .build();
    }

    /**
     * GET /staff — Get all staff of the owner's shop.
     */
    @GetMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Get all staff members of the owner's shop")
    public ApiResponse<List<StaffResponse>> getMyShopStaff(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<StaffResponse>>builder()
                .result(staffService.getMyShopStaff(jwt.getClaim("email")))
                .build();
    }

    /**
     * GET /staff/{id} — Get staff detail.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Get a staff member by ID")
    public ApiResponse<StaffResponse> getStaffById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.getStaffById(id, jwt.getClaim("email")))
                .build();
    }

    /**
     * PUT /staff/{id}/toggle-status — Activate or deactivate a staff account.
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Toggle active/inactive status of a staff member")
    public ApiResponse<StaffResponse> toggleStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.toggleStaffStatus(id, jwt.getClaim("email")))
                .message("Staff status updated")
                .build();
    }

    /**
     * PUT /staff/{id} — Update staff account.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Update a staff member's information")
    public ApiResponse<StaffResponse> updateStaff(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @RequestBody @Valid StaffUpdateRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.updateStaff(id, request, jwt.getClaim("email")))
                .message("Staff updated successfully")
                .build();
    }

    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Get the logged-in staff member's own profile")
    public ApiResponse<StaffResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.getMyProfile(jwt.getClaim("email")))
                .build();
    }

    // ─── Certificates ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/certificates")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Add a certificate to a staff member")
    public ApiResponse<StaffResponse> addCertificate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @RequestBody @Valid com.sang.sourcepattern.dto.request.StaffCertificateRequest request) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.addCertificate(id, request, jwt.getClaim("email")))
                .message("Certificate added successfully")
                .build();
    }

    @DeleteMapping("/certificates/{certId}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Remove a certificate")
    public ApiResponse<Void> removeCertificate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int certId) {
        staffService.removeCertificate(certId, jwt.getClaim("email"));
        return ApiResponse.<Void>builder()
                .message("Certificate removed successfully")
                .build();
    }

    @PutMapping("/certificates/{certId}/verify")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Verify or Reject a certificate")
    public ApiResponse<StaffResponse> verifyCertificate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int certId,
            @RequestParam com.sang.sourcepattern.entity.StaffCertificate.CertificateStatus status) {
        return ApiResponse.<StaffResponse>builder()
                .result(staffService.verifyCertificate(certId, status, jwt.getClaim("email")))
                .message("Certificate status updated to " + status)
                .build();
    }
}
