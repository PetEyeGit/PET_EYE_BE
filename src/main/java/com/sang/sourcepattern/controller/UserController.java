package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.response.PageResponse;
import com.sang.sourcepattern.entity.Notification;
import com.sang.sourcepattern.repository.NotificationRepository;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.sang.sourcepattern.dto.request.UserUpdateRequest;
import com.sang.sourcepattern.dto.request.UserCreationRequest;
import com.sang.sourcepattern.dto.request.PasswordChangeRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.UserResponse;
import com.sang.sourcepattern.service.UserService;

import java.util.List;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.repository.UserRepository;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    NotificationRepository notificationRepository;
    UserRepository userRepository;
    com.sang.sourcepattern.repository.UserVoucherRepository userVoucherRepository;

    @PostMapping("/register")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        log.info("Create User Controller! ");
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request));
        return apiResponse;
    }

    @GetMapping("/{id}")
    ApiResponse<UserResponse> getUser(@PathVariable Integer id) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(id))
                .build();
    }

    @PutMapping("/{id}")
    ApiResponse<UserResponse> updateUser(@PathVariable Integer id, @RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(id, request))
                .build();
    }

    @DeleteMapping("/{id}/avatar")
    ApiResponse<Void> deleteAvatar(@PathVariable Integer id) {
        userService.deleteAvatar(id);
        return ApiResponse.<Void>builder()
                .message("Avatar deleted successfully")
                .build();
    }

    @PostMapping("/{id}/change-password")
    ApiResponse<Void> changePassword(@PathVariable Integer id, @RequestBody @Valid PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ApiResponse.<Void>builder()
                .message("Password changed successfully")
                .build();
    }

    @PatchMapping("/me/acknowledge-upgrade")
    @Transactional
    ApiResponse<Void> acknowledgeUpgrade(@AuthenticationPrincipal Jwt jwt) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        user.setJustUpgraded(false);
        userRepository.save(user);

        return ApiResponse.<Void>builder()
                .message("Upgrade acknowledged")
                .build();
    }

    @GetMapping("/me/vouchers")
    @Transactional(readOnly = true)
    public ApiResponse<List<com.sang.sourcepattern.entity.UserVoucher>> getMyVouchers(@AuthenticationPrincipal Jwt jwt) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        List<com.sang.sourcepattern.entity.UserVoucher> vouchers = userVoucherRepository.findByUserIdAndIsUsedFalse(user.getId());
        return ApiResponse.<List<com.sang.sourcepattern.entity.UserVoucher>>builder()
                .result(vouchers)
                .build();
    }

    // ─── Admin endpoints ─────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<PageResponse<UserResponse>> getAllUsersPaged(
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getAllUsersPaged(page))
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> deactivateUser(@PathVariable Integer id) {
        userService.deactivateUser(id);
        return ApiResponse.<Void>builder()
                .message("User deactivated")
                .build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> activateUser(@PathVariable Integer id) {
        userService.activateUser(id);
        return ApiResponse.<Void>builder()
                .message("User activated")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ApiResponse.<Void>builder()
                .message("User deleted")
                .build();
    }

    // ─── Notification endpoints (user tự xem) ────────────────────────────────

    @GetMapping("/notifications/my")
    ApiResponse<PageResponse<Notification>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }
        
        Page<Notification> pageResult = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, 10));
        return ApiResponse.<PageResponse<Notification>>builder()
                .result(PageResponse.<Notification>builder()
                        .content(pageResult.getContent())
                        .page(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalElements(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .last(pageResult.isLast())
                        .build())
                .build();
    }

    @PatchMapping("/notifications/{id}/read")
    @Transactional
    ApiResponse<Void> markNotificationRead(@PathVariable int id,
                                           @AuthenticationPrincipal Jwt jwt) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }
        
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId() == user.getId()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
        return ApiResponse.<Void>builder().message("Marked as read").build();
    }

    @PatchMapping("/notifications/mark-all-read")
    @Transactional
    ApiResponse<Void> markAllNotificationsRead(@AuthenticationPrincipal Jwt jwt) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        
        return ApiResponse.<Void>builder().message("All marked as read").build();
    }

    @DeleteMapping("/notifications/read")
    @Transactional
    ApiResponse<Void> deleteReadNotifications(@AuthenticationPrincipal Jwt jwt) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        List<Notification> read = notificationRepository.findByUserIdAndIsReadTrue(user.getId());
        notificationRepository.deleteAll(read);
        
        return ApiResponse.<Void>builder().message("Deleted all read notifications").build();
    }

    @DeleteMapping("/notifications/{id}")
    @Transactional
    ApiResponse<Void> deleteNotification(@PathVariable int id, @AuthenticationPrincipal Jwt jwt) {
        String identifier = jwt.getSubject();
        User user;
        if (identifier.contains("@")) {
            user = userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            user = userRepository.findById(Integer.parseInt(identifier))
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        }

        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId() == user.getId()) {
                notificationRepository.delete(n);
            }
        });
        
        return ApiResponse.<Void>builder().message("Deleted notification").build();
    }
}