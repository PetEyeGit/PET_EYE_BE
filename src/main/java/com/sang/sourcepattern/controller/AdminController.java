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
import com.sang.sourcepattern.repository.TransactionRepository;
import com.sang.sourcepattern.repository.WithdrawalRequestRepository;
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
import java.time.YearMonth;
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
    TransactionRepository transactionRepository;
    WithdrawalRequestRepository withdrawalRequestRepository;
    com.sang.sourcepattern.repository.UserVoucherRepository userVoucherRepository;
    com.sang.sourcepattern.service.GoongMapService goongMapService;
    com.sang.sourcepattern.service.WalletService walletService;

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
        BigDecimal totalRevenue = walletService.getAdminBalance();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long inactiveUsers = userRepository.countByActiveFalse();
        long totalShops = shopRepository.count();
        long totalBookings = bookingRepository.count();
        long pendingShops = shopRepository.findAll().stream()
                .filter(s -> s.getStatus() == com.sang.sourcepattern.enums.ShopStatus.PENDING).count();
        long unreadMessages = messageRepository
                .countByShopIdAndChannelTypeAndIsReadFalseAndSenderRoleNot(0, "ADMIN_SUPPORT", "ADMIN");
        long totalVouchers = userVoucherRepository.count();
        long totalBroadcasts = notificationRepository.countDistinctBroadcasts();

        // Period stats
        List<com.sang.sourcepattern.entity.Booking> periodBookingsList = bookingRepository.findActiveAndCompletedBookingsWithServicesBetween(periodStart, periodEnd);
        BigDecimal periodRevenue = BigDecimal.ZERO;
        for (com.sang.sourcepattern.entity.Booking b : periodBookingsList) {
            periodRevenue = periodRevenue.add(walletService.calculateAdminCommissionForBooking(b));
        }
        if (periodRevenue == null) periodRevenue = BigDecimal.ZERO;
        long periodUsers = userRepository.countUsersBetween(periodStart, periodEnd);
        long periodBookings = bookingRepository.countBookingsBetween(periodStart, periodEnd);

        // Calculate Revenue Trend (Current vs Prev period)
        List<com.sang.sourcepattern.entity.Booking> prevPeriodBookingsList = bookingRepository.findActiveAndCompletedBookingsWithServicesBetween(prevStart, prevEnd);
        BigDecimal revPrev = BigDecimal.ZERO;
        for (com.sang.sourcepattern.entity.Booking b : prevPeriodBookingsList) {
            revPrev = revPrev.add(walletService.calculateAdminCommissionForBooking(b));
        }
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
        List<Double> systemBalanceSparkData = new java.util.ArrayList<>();
        List<Long> totalUsersSparkData = new java.util.ArrayList<>();
        List<Long> totalBookingsSparkData = new java.util.ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            LocalDateTime dayStart = LocalDate.now().minusDays(i).atStartOfDay();
            LocalDateTime dayEnd = LocalDate.now().minusDays(i).atTime(23, 59, 59);
            
            List<com.sang.sourcepattern.entity.Booking> dayBookings = bookingRepository.findActiveAndCompletedBookingsWithServicesBetween(dayStart, dayEnd);
            BigDecimal dayRev = BigDecimal.ZERO;
            for (com.sang.sourcepattern.entity.Booking b : dayBookings) {
                dayRev = dayRev.add(walletService.calculateAdminCommissionForBooking(b));
            }
            totalRevenueSparkData.add(dayRev.doubleValue());

            List<com.sang.sourcepattern.entity.Booking> dayFrozenBookings = bookingRepository.findFrozenBookingsWithServicesBetween(dayStart, dayEnd);
            BigDecimal dayFrozen = BigDecimal.ZERO;
            for (com.sang.sourcepattern.entity.Booking b : dayFrozenBookings) {
                BigDecimal fullPrice = BigDecimal.ZERO;
                if (b.getServices() != null) {
                    for (com.sang.sourcepattern.entity.Service s : b.getServices()) {
                        fullPrice = fullPrice.add(walletService.resolveSingleServicePrice(s, b.getPet() != null ? b.getPet().getId() : null, b.getPetWeight()));
                    }
                }
                BigDecimal commission = walletService.calculateAdminCommissionForBooking(b);
                BigDecimal shopFrozen = fullPrice.subtract(commission);
                if (shopFrozen.compareTo(BigDecimal.ZERO) < 0) shopFrozen = BigDecimal.ZERO;
                dayFrozen = dayFrozen.add(shopFrozen);
            }
            systemBalanceSparkData.add(dayFrozen.doubleValue());
            
            totalUsersSparkData.add(userRepository.countUsersBetween(dayStart, dayEnd));
            totalBookingsSparkData.add(bookingRepository.countBookingsBetween(dayStart, dayEnd));
        }

        // Mock sparklines for items without historical query
        List<Integer> totalShopsSparkData = List.of(8, 10, 11, 14, 15, 18, 20, (int)totalShops);
        List<Integer> pendingShopsSparkData = List.of(12, 10, 9, 7, 8, 5, 4, (int)pendingShops);
        List<Integer> unreadMessagesSparkData = List.of(4, 6, 5, 8, 7, 5, 4, (int)unreadMessages);
        List<Integer> totalVouchersSparkData = List.of(0, 5, 12, 15, 20, 22, 28, (int)totalVouchers);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("periodRevenue", periodRevenue);
        result.put("totalRevenueTrend", totalRevenueTrend);
        result.put("totalRevenueTrendUp", totalRevenueTrendUp);
        result.put("totalRevenueSparkData", totalRevenueSparkData);
        result.put("systemBalanceSparkData", systemBalanceSparkData);

        result.put("totalUsers", totalUsers);
        result.put("activeUsers", activeUsers);
        result.put("inactiveUsers", inactiveUsers);
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
        result.put("totalVouchers", totalVouchers);
        result.put("totalVouchersSparkData", totalVouchersSparkData);
        result.put("totalBroadcasts", totalBroadcasts);
        result.put("unreadMessagesSparkData", unreadMessagesSparkData);

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }

    // ─── Dashboard charts ─────────────────────────────────────────────────────

    @GetMapping("/dashboard/revenue-monthly")
    public ApiResponse<List<Map<String, Object>>> getMonthlyRevenue(@RequestParam int year) {
        List<com.sang.sourcepattern.entity.Booking> yearBookings = bookingRepository.findActiveAndCompletedBookingsWithServicesByYear(year);
        
        Map<Integer, BigDecimal> monthMap = new java.util.HashMap<>();
        for (com.sang.sourcepattern.entity.Booking b : yearBookings) {
            LocalDateTime dt = b.getAppointmentDatetime() != null ? b.getAppointmentDatetime() : b.getCreatedAt();
            if (dt != null) {
                int month = dt.getMonthValue();
                BigDecimal commission = walletService.calculateAdminCommissionForBooking(b);
                monthMap.put(month, monthMap.getOrDefault(month, BigDecimal.ZERO).add(commission));
            }
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("month", m);
            item.put("revenue", monthMap.getOrDefault(m, BigDecimal.ZERO));
            result.add(item);
        }

        return ApiResponse.<List<Map<String, Object>>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/dashboard/bookings-weekly")
    public ApiResponse<List<DailyBookingResponse>> getWeeklyBookings(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate today = LocalDate.now();
        LocalDate end = (endDate != null) ? endDate : today;
        LocalDate start = (startDate != null) ? startDate : end.minusDays(6);

        if (start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<com.sang.sourcepattern.entity.Booking> allBookings = bookingRepository.findAll();
        Map<String, Long> countMap = new java.util.HashMap<>();

        for (com.sang.sourcepattern.entity.Booking b : allBookings) {
            if ("CANCELLED".equalsIgnoreCase(b.getStatus())) {
                continue;
            }

            LocalDate targetDate = null;
            // 1. Ưu tiên ngày tạo booking nếu nằm trong khoảng thời gian đã chọn
            if (b.getCreatedAt() != null) {
                LocalDate cDate = b.getCreatedAt().toLocalDate();
                if (!cDate.isBefore(start) && !cDate.isAfter(end)) {
                    targetDate = cDate;
                }
            }
            // 2. Nếu ngày tạo không nằm trong khoảng, kiểm tra ngày hẹn dịch vụ
            if (targetDate == null && b.getAppointmentDatetime() != null) {
                LocalDate aDate = b.getAppointmentDatetime().toLocalDate();
                if (!aDate.isBefore(start) && !aDate.isAfter(end)) {
                    targetDate = aDate;
                }
            }
            // 3. Fallback bất kỳ mốc thời gian nào của booking
            if (targetDate == null) {
                LocalDateTime fallback = b.getAppointmentDatetime() != null ? b.getAppointmentDatetime() : b.getCreatedAt();
                if (fallback != null) {
                    LocalDate fDate = fallback.toLocalDate();
                    if (!fDate.isBefore(start) && !fDate.isAfter(end)) {
                        targetDate = fDate;
                    }
                }
            }

            if (targetDate != null) {
                String dateStr = targetDate.format(fmt);
                countMap.put(dateStr, countMap.getOrDefault(dateStr, 0L) + 1L);
            }
        }

        // Trả đủ danh sách ngày từ start đến end
        List<DailyBookingResponse> result = new java.util.ArrayList<>();
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            String dateStr = curr.format(fmt);
            result.add(DailyBookingResponse.builder()
                    .date(dateStr)
                    .count(countMap.getOrDefault(dateStr, 0L))
                    .build());
            curr = curr.plusDays(1);
        }

        return ApiResponse.<List<DailyBookingResponse>>builder().result(result).build();
    }

    @GetMapping("/dashboard/monthly-history")
    public ApiResponse<Map<String, List<Map<String, Object>>>> getMonthlyHistory(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) String status) {

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        List<com.sang.sourcepattern.entity.Booking> allMonthBookings = bookingRepository.findAllBookingsWithServicesByMonthAndYear(year, month);
        List<com.sang.sourcepattern.entity.Booking> monthBookings;

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            monthBookings = allMonthBookings.stream()
                    .filter(b -> b.getStatus() != null && b.getStatus().equalsIgnoreCase(status))
                    .toList();
        } else {
            monthBookings = allMonthBookings.stream()
                    .filter(b -> b.getStatus() != null && !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                    .toList();
        }

        Map<Integer, BigDecimal> dailyRevenueMap = new java.util.HashMap<>();
        Map<Integer, BigDecimal> dailyFrozenMap = new java.util.HashMap<>();
        Map<Integer, Long> dailyBookingCountMap = new java.util.HashMap<>();

        for (com.sang.sourcepattern.entity.Booking b : monthBookings) {
            LocalDateTime dt = b.getAppointmentDatetime() != null ? b.getAppointmentDatetime() : b.getCreatedAt();
            if (dt != null) {
                int day = dt.getDayOfMonth();
                dailyBookingCountMap.put(day, dailyBookingCountMap.getOrDefault(day, 0L) + 1L);

                BigDecimal commission = walletService.calculateAdminCommissionForBooking(b);
                dailyRevenueMap.put(day, dailyRevenueMap.getOrDefault(day, BigDecimal.ZERO).add(commission));

                BigDecimal fullPrice = BigDecimal.ZERO;
                if (b.getServices() != null && !b.getServices().isEmpty()) {
                    for (com.sang.sourcepattern.entity.Service s : b.getServices()) {
                        BigDecimal p = walletService.resolveSingleServicePrice(s, b.getPet() != null ? b.getPet().getId() : null, b.getPetWeight());
                        if (p == null || p.compareTo(BigDecimal.ZERO) == 0) {
                            p = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
                        }
                        fullPrice = fullPrice.add(p);
                    }
                }
                BigDecimal shopFrozen = fullPrice.subtract(commission);
                if (shopFrozen.compareTo(BigDecimal.ZERO) < 0) shopFrozen = BigDecimal.ZERO;
                dailyFrozenMap.put(day, dailyFrozenMap.getOrDefault(day, BigDecimal.ZERO).add(shopFrozen));
            }
        }

        List<Map<String, Object>> revenueSeries = new java.util.ArrayList<>();
        List<Map<String, Object>> frozenSeries = new java.util.ArrayList<>();
        List<Map<String, Object>> bookingSeries = new java.util.ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            Map<String, Object> revPoint = new java.util.HashMap<>();
            revPoint.put("day", "Ngày " + i);
            revPoint.put("value", dailyRevenueMap.getOrDefault(i, BigDecimal.ZERO));
            revenueSeries.add(revPoint);

            Map<String, Object> frozenPoint = new java.util.HashMap<>();
            frozenPoint.put("day", "Ngày " + i);
            frozenPoint.put("value", dailyFrozenMap.getOrDefault(i, BigDecimal.ZERO));
            frozenSeries.add(frozenPoint);

            Map<String, Object> bookPoint = new java.util.HashMap<>();
            bookPoint.put("day", "Ngày " + i);
            bookPoint.put("value", dailyBookingCountMap.getOrDefault(i, 0L));
            bookingSeries.add(bookPoint);
        }

        List<Object[]> usersData = userRepository.userCountByDateRange(year, month);
        List<Object[]> activeUsersData = userRepository.activeUserCountByDateRange(year, month);
        List<Object[]> inactiveUsersData = userRepository.inactiveUserCountByDateRange(year, month);
        List<Object[]> shopsData = shopRepository.shopCountByDateRange(year, month);
        List<Object[]> withdrawalData = withdrawalRequestRepository.withdrawalCountByDateRange(year, month);
        List<Object[]> pendingShopsData = shopRepository.pendingShopCountByDateRange(year, month);
        List<Object[]> messagesData = messageRepository.messageCountByDateRange(year, month);
        List<Object[]> vouchersData = userVoucherRepository.voucherCountByDateRange(year, month);

        Map<String, List<Map<String, Object>>> result = new java.util.HashMap<>();
        
        result.put("totalRevenue", revenueSeries);
        result.put("systemBalance", frozenSeries);
        result.put("totalUsers", buildDailySeries(daysInMonth, usersData));
        result.put("activeUsers", buildDailySeries(daysInMonth, activeUsersData));
        result.put("inactiveUsers", buildDailySeries(daysInMonth, inactiveUsersData));
        result.put("totalShops", buildDailySeries(daysInMonth, shopsData));
        result.put("totalBookings", bookingSeries);
        result.put("pendingWithdrawals", buildDailySeries(daysInMonth, withdrawalData));
        result.put("pendingShops", buildDailySeries(daysInMonth, pendingShopsData));
        result.put("unreadMessages", buildDailySeries(daysInMonth, messagesData));
        result.put("totalVouchers", buildDailySeries(daysInMonth, vouchersData));

        return ApiResponse.<Map<String, List<Map<String, Object>>>>builder()
                .result(result)
                .build();
    }

    private List<Map<String, Object>> buildDailySeries(int daysInMonth, List<Object[]> queryData) {
        Map<Integer, Number> map = new java.util.HashMap<>();
        for (Object[] row : queryData) {
            java.sql.Date date = (java.sql.Date) row[0];
            int day = date.toLocalDate().getDayOfMonth();
            Number val = (Number) row[1];
            map.put(day, val);
        }

        List<Map<String, Object>> series = new java.util.ArrayList<>();
        for (int i = 1; i <= daysInMonth; i++) {
            Map<String, Object> point = new java.util.HashMap<>();
            point.put("day", "Ngày " + i);
            point.put("value", map.getOrDefault(i, 0));
            series.add(point);
        }
        return series;
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
