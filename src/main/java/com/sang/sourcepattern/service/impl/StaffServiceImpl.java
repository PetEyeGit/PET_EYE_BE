package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.StaffCreationRequest;
import com.sang.sourcepattern.dto.request.StaffUpdateRequest;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.Staff;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.StaffCertificateRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.entity.StaffCertificate;
import com.sang.sourcepattern.dto.response.StaffCertificateResponse;
import com.sang.sourcepattern.dto.request.StaffCertificateRequest;
import com.sang.sourcepattern.service.StaffService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StaffServiceImpl implements StaffService {

    StaffRepository staffRepository;
    ShopRepository shopRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    StaffCertificateRepository staffCertificateRepository;
    PasswordEncoder passwordEncoder;

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Shop resolveOwnerShop(String ownerEmail) {
        return shopRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
    }

    private Staff resolveStaffInOwnerShop(int staffId, String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        if (staff.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);
        return staff;
    }

    @Override
    public StaffResponse getMyProfile(String email) {
        Staff staff = staffRepository.findByUserEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return toResponse(staff);
    }

    private StaffResponse toResponse(Staff s) {
        List<StaffCertificate> certs = staffCertificateRepository.findByStaffId(s.getId());
        List<StaffCertificateResponse> certResponses = certs.stream()
                .map(c -> StaffCertificateResponse.builder()
                        .id(c.getId())
                        .certificateName(c.getCertificateName())
                        .imageUrl(c.getImageUrl())
                        .issueDate(c.getIssueDate())
                        .expiryDate(c.getExpiryDate())
                        .status(c.getStatus())
                        .build())
                .collect(Collectors.toList());

        return StaffResponse.builder()
                .id(s.getId())
                .shopId(s.getShop().getId())
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .email(s.getUser() != null ? s.getUser().getEmail() : null)
                .fullName(s.getFullName())
                .role(s.getRole())
                .phone(s.getPhone())
                .specialization(s.getSpecialization())
                .isActive(s.isActive())
                .certificates(certResponses)
                .build();
    }

    // ─── Create Staff ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public StaffResponse createStaff(StaffCreationRequest request, String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);

        // Check email uniqueness
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.STAFF_EMAIL_EXISTED);
        }

        // 1. Create a User account with STAFF role
        Role staffRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(staffRole);

        User staffUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .active(true)
                .emailVerified(true) // Staff accounts are pre-verified by owner
                .roles(roles)
                .build();

        staffUser = userRepository.save(staffUser);

        // 2. Create Staff profile linked to shop and user account
        Staff staff = Staff.builder()
                .shop(shop)
                .user(staffUser)
                .fullName(request.getFullName())
                .role(request.getRole())
                .phone(request.getPhone())
                .specialization(request.getSpecialization())
                .isActive(true)
                .build();

        staff = staffRepository.save(staff);

        // 3. Save certificates if provided
        if (request.getCertificates() != null && !request.getCertificates().isEmpty()) {
            for (StaffCertificateRequest certReq : request.getCertificates()) {
                StaffCertificate cert = StaffCertificate.builder()
                        .staff(staff)
                        .certificateName(certReq.getCertificateName())
                        .imageUrl(certReq.getImageUrl())
                        .issueDate(certReq.getIssueDate())
                        .expiryDate(certReq.getExpiryDate())
                        .status(StaffCertificate.CertificateStatus.PENDING)
                        .build();
                staffCertificateRepository.save(cert);
            }
        }

        log.info("Staff created: {} for shop: {}", staff.getFullName(), shop.getShopName());

        return toResponse(staff);
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    @Override
    public List<StaffResponse> getMyShopStaff(String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);
        return staffRepository.findByShopId(shop.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public StaffResponse getStaffById(int staffId, String ownerEmail) {
        return toResponse(resolveStaffInOwnerShop(staffId, ownerEmail));
    }

    // ─── Toggle Status ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public StaffResponse toggleStaffStatus(int staffId, String ownerEmail) {
        Staff staff = resolveStaffInOwnerShop(staffId, ownerEmail);
        boolean newStatus = !staff.isActive();
        staff.setActive(newStatus);

        // Mirror status on linked User account
        if (staff.getUser() != null) {
            staff.getUser().setActive(newStatus);
            userRepository.save(staff.getUser());
        }

        staffRepository.save(staff);
        log.info("Staff {} status toggled to: {}", staff.getFullName(), newStatus ? "ACTIVE" : "INACTIVE");
        return toResponse(staff);
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(int staffId, StaffUpdateRequest request, String ownerEmail) {
        Staff staff = resolveStaffInOwnerShop(staffId, ownerEmail);

        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setRole(request.getRole());
        staff.setSpecialization(request.getSpecialization());

        // Update linked User account too
        if (staff.getUser() != null) {
            staff.getUser().setFullName(request.getFullName());
            staff.getUser().setPhone(request.getPhone());
            userRepository.save(staff.getUser());
        }

        staff = staffRepository.save(staff);
        log.info("Staff {} updated by owner {}", staff.getFullName(), ownerEmail);
        return toResponse(staff);
    }

    // ─── Certificates ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public StaffResponse addCertificate(int staffId, StaffCertificateRequest request, String userEmail) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Authorization:
        // 1. If SHOP_OWNER: check if staff belongs to their shop
        // 2. If STAFF: check if they are updating themselves
        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isOwner = requester.getRoles().stream().anyMatch(r -> r.getName().equals("SHOP_OWNER"));
        
        if (isOwner) {
            Shop shop = resolveOwnerShop(userEmail);
            if (staff.getShop().getId() != shop.getId())
                throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);
        } else {
            // Requester is STAFF
            if (staff.getUser() == null || !staff.getUser().getEmail().equals(userEmail))
                throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        StaffCertificate cert = StaffCertificate.builder()
                .staff(staff)
                .certificateName(request.getCertificateName())
                .imageUrl(request.getImageUrl())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .status(StaffCertificate.CertificateStatus.PENDING)
                .build();

        staffCertificateRepository.save(cert);
        return toResponse(staff);
    }

    @Override
    @Transactional
    public void removeCertificate(int certId, String userEmail) {
        StaffCertificate cert = staffCertificateRepository.findById(certId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        
        Staff staff = cert.getStaff();
        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        
        boolean isOwner = requester.getRoles().stream().anyMatch(r -> r.getName().equals("SHOP_OWNER"));
        
        if (isOwner) {
            Shop shop = resolveOwnerShop(userEmail);
            if (staff.getShop().getId() != shop.getId())
                throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);
        } else {
            if (staff.getUser() == null || !staff.getUser().getEmail().equals(userEmail))
                throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        staffCertificateRepository.delete(cert);
    }

    @Override
    @Transactional
    public StaffResponse verifyCertificate(int certId, StaffCertificate.CertificateStatus status, String ownerEmail) {
        StaffCertificate cert = staffCertificateRepository.findById(certId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        resolveStaffInOwnerShop(cert.getStaff().getId(), ownerEmail);

        cert.setStatus(status);
        staffCertificateRepository.save(cert);

        return toResponse(cert.getStaff());
    }
}
