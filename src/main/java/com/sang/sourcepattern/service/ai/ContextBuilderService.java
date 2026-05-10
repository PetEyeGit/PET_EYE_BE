package com.sang.sourcepattern.service.ai;

import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.ShopDashboardResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.ServiceRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.BookingService;
import com.sang.sourcepattern.service.ShopService;
import com.sang.sourcepattern.service.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContextBuilderService {

    private final ShopService shopService;
    private final BookingService bookingService;
    private final StaffService staffService;
    private final ServiceRepository serviceRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    /**
     * Build business context string for the given agentType.
     * USER_CHAT returns empty string (uses tools instead).
     */
    public String buildContext(String agentType, Jwt jwt) {
        try {
            return switch (agentType) {
                case "USER_CHAT" -> "";
                case "SHOP_ASSISTANT" -> buildShopContext(jwt);
                case "ADMIN_ASSISTANT" -> buildAdminContext();
                default -> "";
            };
        } catch (Exception e) {
            log.warn("[ContextBuilder] Failed to build context for {}: {}", agentType, e.getMessage());
            return "=== Không thể tải dữ liệu ===";
        }
    }

    // ── Shop context ──────────────────────────────────────────────────────────

    private String buildShopContext(Jwt jwt) {
        String email = jwt.getClaim("email");
        Shop shop = shopRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        ShopDashboardResponse dashboard = shopService.getShopDashboard(email);
        List<BookingResponse> bookings = bookingService.getShopBookings(email, null, null);
        List<StaffResponse> staff = staffService.getMyShopStaff(email);
        List<com.sang.sourcepattern.entity.Service> services = serviceRepository.findByShopIdAndActiveTrue(shop.getId());

        // Booking stats
        Map<String, Long> statusCount = bookings.stream()
                .collect(Collectors.groupingBy(BookingResponse::getStatus, Collectors.counting()));

        // Service usage
        Map<String, Long> serviceUsage = bookings.stream()
                .collect(Collectors.groupingBy(BookingResponse::getServiceName, Collectors.counting()));

        String topServices = serviceUsage.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + "(" + e.getValue() + " lần)")
                .collect(Collectors.joining(", "));

        // Top customers by booking count
        String topCustomers = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getPetName() != null ? b.getPetName() : "Unknown",
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + ":" + e.getValue() + " lần")
                .collect(Collectors.joining(", "));

        long activeStaff = staff.stream().filter(StaffResponse::isActive).count();

        return """
                === DỮ LIỆU SHOP (cập nhật: %s) ===
                SHOP: %s | Loại: %s | Rating: %.1f/5 | %s
                Địa chỉ: %s, %s

                DOANH THU:
                - Tổng: %sđ | Tháng này: %sđ

                LỊCH HẸN:
                - Tổng: %d | Theo trạng thái: %s

                TOP KHÁCH HÀNG (theo tên thú cưng): %s

                DỊCH VỤ HOT: %s
                Dịch vụ chưa ai đặt: %s

                NHÂN VIÊN: %d tổng (%d active)
                === HẾT DỮ LIỆU ===
                """.formatted(
                LocalDate.now(),
                shop.getShopName(), shop.getShopType(),
                shop.getRatingAvg(),
                shop.isVerified() ? "✅ Đã xác minh" : "⚠️ Chưa xác minh",
                shop.getAddress(), shop.getCity(),
                fmt(dashboard.getTotalRevenue()),
                fmt(dashboard.getRevenueThisMonth()),
                bookings.size(),
                statusCount.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ")),
                topCustomers.isEmpty() ? "Chưa có" : topCustomers,
                topServices.isEmpty() ? "Chưa có" : topServices,
                services.stream()
                        .filter(s -> !serviceUsage.containsKey(s.getServiceName()))
                        .map(com.sang.sourcepattern.entity.Service::getServiceName)
                        .collect(Collectors.joining(", "))
                        .isEmpty() ? "Không có" : services.stream()
                        .filter(s -> !serviceUsage.containsKey(s.getServiceName()))
                        .map(com.sang.sourcepattern.entity.Service::getServiceName)
                        .collect(Collectors.joining(", ")),
                staff.size(), activeStaff
        );
    }

    // ── Admin context ─────────────────────────────────────────────────────────

    private String buildAdminContext() {
        List<ShopResponse> allShops = shopService.getAllShops();
        List<User> allUsers = userRepository.findAll();

        long verifiedShops = allShops.stream().filter(ShopResponse::isVerified).count();
        long pendingShops = allShops.stream().filter(s -> !s.isVerified()).count();

        Map<String, Long> shopsByType = allShops.stream()
                .collect(Collectors.groupingBy(ShopResponse::getShopType, Collectors.counting()));

        Map<String, Long> roleCount = allUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getRoles().isEmpty() ? "UNKNOWN"
                                : u.getRoles().iterator().next().getName(),
                        Collectors.counting()
                ));

        String topRatedShops = allShops.stream()
                .filter(ShopResponse::isVerified)
                .sorted((a, b) -> Float.compare(b.getRatingAvg(), a.getRatingAvg()))
                .limit(5)
                .map(s -> s.getShopName() + "(" + s.getRatingAvg() + "⭐," + s.getCity() + ")")
                .collect(Collectors.joining(", "));

        String pendingList = allShops.stream()
                .filter(s -> !s.isVerified())
                .limit(10)
                .map(s -> s.getShopName() + "-" + s.getCity())
                .collect(Collectors.joining(", "));

        return """
                === DỮ LIỆU HỆ THỐNG PETEYE (cập nhật: %s) ===
                TỔNG QUAN:
                - Tổng người dùng: %d
                - Tổng shop: %d (đã duyệt: %d, chờ duyệt: %d)

                SHOP:
                - Phân loại: %s
                - Top shop đánh giá cao: %s
                - Shop chờ duyệt: %s

                MEMBER:
                - Tổng: %d
                - Theo role: %s
                === HẾT DỮ LIỆU ===
                """.formatted(
                LocalDate.now(),
                allUsers.size(),
                allShops.size(), verifiedShops, pendingShops,
                shopsByType.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ")),
                topRatedShops.isEmpty() ? "Chưa có" : topRatedShops,
                pendingList.isEmpty() ? "Không có" : pendingList,
                allUsers.size(),
                roleCount.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", "))
        );
    }

    private String fmt(java.math.BigDecimal n) {
        if (n == null) return "0";
        return String.format("%,.0f", n.doubleValue()).replace(",", ".");
    }
}
