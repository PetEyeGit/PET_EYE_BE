package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.SendNotificationRequest;
import com.sang.sourcepattern.dto.response.DailyBookingResponse;
import com.sang.sourcepattern.dto.response.MonthlyRevenueResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
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
    com.sang.sourcepattern.service.GoongMapService goongMapService;

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime periodEnd = (endDate != null) ? endDate.atTime(23, 59, 59) : now;
        LocalDateTime periodStart = (startDate != null) ? startDate.atStartOfDay() : periodEnd.minusDays(30);

        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(periodStart.toLocalDate(), periodEnd.toLocalDate()) + 1;
        if (daysDiff <= 0) daysDiff = 1;

        LocalDateTime prevStart = periodStart.minusDays(daysDiff);
        LocalDateTime prevEnd = periodStart.minusSeconds(1);

        // Core stats
        BigDecimal totalRevenue = bookingRepository.sumTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        long totalUsers = userRepository.count();
        long totalShops = shopRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingShops = shopRepository.findAll().stream()
                .filter(s -> s.getStatus() == com.sang.sourcepattern.enums.ShopStatus.PENDING).count();
        long unreadMessages = messageRepository
                .countByShopIdAndChannelTypeAndIsReadFalseAndSenderRoleNot(0, "ADMIN_SUPPORT", "ADMIN");

        // Period stats
        BigDecimal periodRevenue = bookingRepository.sumRevenueBetween(periodStart, periodEnd);
        if (periodRevenue == null) periodRevenue = BigDecimal.ZERO;
        long periodUsers = userRepository.countUsersBetween(periodStart, periodEnd);
        long periodBookings = bookingRepository.countBookingsBetween(periodStart, periodEnd);

        // Calculate Revenue Trend (Current vs Prev period)
        BigDecimal revPrev = bookingRepository.sumRevenueBetween(prevStart, prevEnd);
        double revTrendVal = 0.0;
        if (revPrev != null && revPrev.compareTo(BigDecimal.ZERO) > 0 && periodRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revTrendVal = periodRevenue.subtract(revPrev).doubleValue() / revPrev.doubleValue() * 100;
        } else if ((revPrev == null || revPrev.compareTo(BigDecimal.ZERO) == 0) && periodRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revTrendVal = 100.0;
        }
        String totalRevenueTrend = String.format("%s%.1f%%", revTrendVal >= 0 ? "+" : "", revTrendVal);
        Boolean totalRevenueTrendUp = revTrendVal >= 0;

        // Calculate Users Trend
        long usersPrev = userRepository.countUsersBetween(prevStart, prevEnd);
        double usersTrendVal = 0.0;
        if (usersPrev > 0) {
            usersTrendVal = (double) (periodUsers - usersPrev) / usersPrev * 100;
        } else if (usersPrev == 0 && periodUsers > 0) {
            usersTrendVal = 100.0;
        }
        String totalUsersTrend = String.format("%s%.1f%%", usersTrendVal >= 0 ? "+" : "", usersTrendVal);
        Boolean totalUsersTrendUp = usersTrendVal >= 0;

        // Calculate Bookings Trend
        long bookingsPrev = bookingRepository.countBookingsBetween(prevStart, prevEnd);
        double bookingsTrendVal = 0.0;
        if (bookingsPrev > 0) {
            bookingsTrendVal = (double) (periodBookings - bookingsPrev) / bookingsPrev * 100;
        } else if (bookingsPrev == 0 && periodBookings > 0) {
            bookingsTrendVal = 100.0;
        }
        String totalBookingsTrend = String.format("%s%.1f%%", bookingsTrendVal >= 0 ? "+" : "", bookingsTrendVal);
        Boolean totalBookingsTrendUp = bookingsTrendVal >= 0;

        // Calculate Sparklines (Last 8 days)
        List<Double> totalRevenueSparkData = new java.util.ArrayList<>();
        List<Long> totalUsersSparkData = new java.util.ArrayList<>();
        List<Long> totalBookingsSparkData = new java.util.ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            LocalDateTime dayStart = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = LocalDate.now().minusDays(i).atTime(23, 59, 59);
            
            BigDecimal dayRev = bookingRepository.sumRevenueBetween(dayStart, dayEnd);
            totalRevenueSparkData.add(dayRev != null ? dayRev.doubleValue() : 0.0);
            
            totalUsersSparkData.add(userRepository.countUsersBetween(dayStart, dayEnd));
            totalBookingsSparkData.add(bookingRepository.countBookingsBetween(dayStart, dayEnd));
        }

        // Mock sparklines for items without historical query
        List<Integer> totalShopsSparkData = List.of(8, 10, 11, 14, 15, 18, 20, (int)totalShops);
        List<Integer> pendingShopsSparkData = List.of(12, 10, 9, 7, 8, 5, 4, (int)pendingShops);
        List<Integer> unreadMessagesSparkData = List.of(4, 6, 5, 8, 7, 5, 4, (int)unreadMessages);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("periodRevenue", periodRevenue);
        result.put("totalRevenueTrend", totalRevenueTrend);
        result.put("totalRevenueTrendUp", totalRevenueTrendUp);
        result.put("totalRevenueSparkData", totalRevenueSparkData);

        result.put("totalUsers", totalUsers);
        result.put("periodUsers", periodUsers);
        result.put("totalUsersTrend", totalUsersTrend);
        result.put("totalUsersTrendUp", totalUsersTrendUp);
        result.put("totalUsersSparkData", totalUsersSparkData);

        result.put("totalShops", totalShops);
        result.put("totalShopsTrend", "+4.5%");
        result.put("totalShopsTrendUp", true);
        result.put("totalShopsSparkData", totalShopsSparkData);

        result.put("totalBookings", totalBookings);
        result.put("periodBookings", periodBookings);
        result.put("totalBookingsTrend", totalBookingsTrend);
        result.put("totalBookingsTrendUp", totalBookingsTrendUp);
        result.put("totalBookingsSparkData", totalBookingsSparkData);

        result.put("pendingShops", pendingShops);
        result.put("pendingShopsTrend", "-18.4%");
        result.put("pendingShopsTrendUp", false);
        result.put("pendingShopsSparkData", pendingShopsSparkData);

        result.put("unreadMessages", unreadMessages);

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }

    // ─── Dashboard charts ─────────────────────────────────────────────────────

    @GetMapping("/dashboard/revenue-monthly")
    public ApiResponse<List<MonthlyRevenueResponse>> getMonthlyRevenue(
            @RequestParam(required = false) Integer year) {

        int targetYear = (year != null) ? year : Year.now().getValue();

        // Build map month -> revenue từ query
        Map<Integer, BigDecimal> revenueMap = new java.util.HashMap<>();
        for (Object[] row : bookingRepository.adminCommissionByMonth(targetYear)) {
            int month = ((Number) row[0]).intValue();
            BigDecimal revenue = new BigDecimal(row[1].toString());
            revenueMap.put(month, revenue);
        }

        // Trả đủ 12 tháng, tháng không có thì 0
        List<MonthlyRevenueResponse> result = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(MonthlyRevenueResponse.builder()
                    .month(m)
                    .revenue(revenueMap.getOrDefault(m, BigDecimal.ZERO))
                    .build());
        }

        return ApiResponse.<List<MonthlyRevenueResponse>>builder().result(result).build();
    }

    @GetMapping("/dashboard/bookings-weekly")
    public ApiResponse<List<DailyBookingResponse>> getWeeklyBookings() {

        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(6).atStartOfDay();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Build map date -> count từ query
        Map<String, Long> countMap = new java.util.HashMap<>();
        for (Object[] row : bookingRepository.bookingCountByDate(from)) {
            String date = row[0].toString().substring(0, 10); // đảm bảo format yyyy-MM-dd
            long count = ((Number) row[1]).longValue();
            countMap.put(date, count);
        }

        // Trả đủ 7 ngày, ngày không có thì 0
        List<DailyBookingResponse> result = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String date = today.minusDays(i).format(fmt);
            result.add(DailyBookingResponse.builder()
                    .date(date)
                    .count(countMap.getOrDefault(date, 0L))
                    .build());
        }

        return ApiResponse.<List<DailyBookingResponse>>builder().result(result).build();
    }

    // ─── Notifications ───────────────────────────────────────────────────────    /** Admin xem danh sách thông báo đã gửi — group theo đợt gửi, phân trang */
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
                        .notificationType(n.getNotificationType() != null ? n.getNotificationType().name() : null)
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
                        .notificationType(request.getNotificationType() != null
                                ? Notification.NotificationType.valueOf(request.getNotificationType().name())
                                : Notification.NotificationType.GENERAL)
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

    // ─── Geocode all shops ───────────────────────────────────────────────────

    /** Admin geocode lại tất cả shop để lấy tọa độ từ Goong API */
    @PostMapping("/shops/geocode-all")
    public ApiResponse<Map<String, Object>> geocodeAllShops() {
        List<com.sang.sourcepattern.entity.Shop> shops = shopRepository.findAll();
        int success = 0;
        int failed = 0;

        for (com.sang.sourcepattern.entity.Shop shop : shops) {
            if (shop.getAddress() != null && !shop.getAddress().isEmpty()) {
                com.sang.sourcepattern.dto.response.goong.LatLong location = 
                        goongMapService.geocodeAddress(shop.getAddress());
                
                if (location != null) {
                    shop.setLatitude(location.getLatitude());
                    shop.setLongitude(location.getLongitude());
                    shopRepository.save(shop);
                    success++;
                } else {
                    failed++;
                }
            } else {
                failed++;
            }
        }

        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                        "total", shops.size(),
                        "success", success,
                        "failed", failed
                ))
                .message("Geocoded " + success + " shops successfully")
                .build();
    }
}
