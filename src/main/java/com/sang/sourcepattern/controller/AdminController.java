package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.SendNotificationRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.NotificationBroadcastResponse;
import com.sang.sourcepattern.dto.response.PageResponse;
import com.sang.sourcepattern.entity.Notification;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.MessageRepository;
import com.sang.sourcepattern.repository.NotificationRepository;
import com.sang.sourcepattern.repository.PaymentRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sang.sourcepattern.entity.User;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
public class AdminController {

    PaymentRepository paymentRepository;
    BookingRepository bookingRepository;
    UserRepository userRepository;
    ShopRepository shopRepository;
    NotificationRepository notificationRepository;
    MessageRepository messageRepository;

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        long totalUsers = userRepository.count();
        long totalShops = shopRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingShops = shopRepository.findAll().stream()
                .filter(s -> !s.isVerified()).count();
        long unreadMessages = messageRepository
                .countByShopIdAndIsReadFalseAndSenderRoleNot(0, "ADMIN");

        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                        "totalRevenue", totalRevenue,
                        "totalUsers", totalUsers,
                        "totalShops", totalShops,
                        "totalBookings", totalBookings,
                        "pendingShops", pendingShops,
                        "unreadMessages", unreadMessages
                ))
                .build();
    }

    // ─── Notifications ───────────────────────────────────────────────────────

    /** Admin xem danh sách thông báo đã gửi — group theo đợt gửi, phân trang */
    @GetMapping("/notifications")
    public ApiResponse<PageResponse<NotificationBroadcastResponse>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page) {

        Page<Notification> pageResult = notificationRepository
                .findDistinctBroadcasts(PageRequest.of(page, 10));

        List<NotificationBroadcastResponse> content = pageResult.getContent().stream()
                .map(n -> NotificationBroadcastResponse.builder()
                        .broadcastId(n.getBroadcastId())
                        .title(n.getTitle())
                        .content(n.getContent())
                        .totalSent(notificationRepository.countByBroadcastId(n.getBroadcastId()))
                        .totalRead(notificationRepository.countByBroadcastIdAndIsReadTrue(n.getBroadcastId()))
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();

        return ApiResponse.<PageResponse<NotificationBroadcastResponse>>builder()
                .result(PageResponse.<NotificationBroadcastResponse>builder()
                        .content(content)
                        .page(pageResult.getNumber())
                        .size(pageResult.getSize())
                        .totalElements(pageResult.getTotalElements())
                        .totalPages(pageResult.getTotalPages())
                        .last(pageResult.isLast())
                        .build())
                .build();
    }

    /** Admin gửi thông báo — tạo 1 bản/user nhưng group bằng broadcastId */
    @PostMapping("/notifications")
    public ApiResponse<Void> sendNotification(@RequestBody @Valid SendNotificationRequest request) {
        List<User> targets = switch (request.getTargetType()) {
            case SINGLE -> {
                if (request.getUserId() == null)
                    throw new AppException(ErrorCode.USER_NOT_EXISTED);
                User user = userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                yield List.of(user);
            }
            case ALL_USERS -> userRepository.findByRoleName("USER");
            case ALL_SHOPS -> userRepository.findByRoleName("SHOP_OWNER");
            case ALL -> userRepository.findAll();
        };

        String broadcastId = UUID.randomUUID().toString();

        List<Notification> notifications = targets.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .title(request.getTitle())
                        .content(request.getContent())
                        .broadcastId(broadcastId)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);

        return ApiResponse.<Void>builder()
                .message("Sent to " + notifications.size() + " user(s)")
                .build();
    }

    /** Xóa toàn bộ thông báo thuộc 1 đợt gửi */
    @DeleteMapping("/notifications/{broadcastId}")
    public ApiResponse<Void> deleteNotification(@PathVariable String broadcastId) {
        List<Notification> group = notificationRepository.findByBroadcastId(broadcastId);
        notificationRepository.deleteAll(group);
        return ApiResponse.<Void>builder()
                .message("Deleted " + group.size() + " notification(s)")
                .build();
    }
}
