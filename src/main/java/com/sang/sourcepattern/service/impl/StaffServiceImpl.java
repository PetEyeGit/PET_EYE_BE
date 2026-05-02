package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.StaffCreationRequest;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.Staff;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.repository.UserRepository;
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

    private StaffResponse toResponse(Staff s) {
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
}
