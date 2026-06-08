package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.response.goong.LatLong;
import com.sang.sourcepattern.enums.ShopStatus;
import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.request.ShopUpdateRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.entity.Role;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.mapper.ShopMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.experimental.NonFinal;
import com.sang.sourcepattern.dto.response.ShopDashboardResponse;
import com.sang.sourcepattern.dto.response.CustomerDetailResponse;
import com.sang.sourcepattern.dto.response.CustomerItemResponse;
import com.sang.sourcepattern.dto.response.ShopCustomerResponse;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.mapper.BookingMapper;
import com.sang.sourcepattern.mapper.PetMapper;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.PetRepository;
import com.sang.sourcepattern.repository.RoleRepository;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.repository.StaffCertificateRepository;
import com.sang.sourcepattern.repository.UserTokenRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.repository.NotificationRepository;
import com.sang.sourcepattern.dto.response.StaffCertificateResponse;
import com.sang.sourcepattern.service.ShopService;
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

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShopServiceImpl implements ShopService {

    ShopRepository shopRepository;
    UserRepository userRepository;
    UserTokenRepository userTokenRepository;
    RoleRepository roleRepository;
    BookingRepository bookingRepository;
    PetRepository petRepository;
    @NonFinal
    @Autowired(required = false)
    ShopMapper shopMapper;
    PetMapper petMapper;
    BookingMapper bookingMapper;
    PasswordEncoder passwordEncoder;
    StaffRepository staffRepository;
    StaffCertificateRepository staffCertificateRepository;
    com.sang.sourcepattern.repository.TransactionRepository transactionRepository;
    NotificationRepository notificationRepository;

    com.sang.sourcepattern.service.EmailService emailService;
    com.sang.sourcepattern.service.GoongMapService goongMapService;

    @Override
    public CustomerDetailResponse getCustomerDetail(String ownerEmail, int customerId) {
        if (shopMapper == null) shopMapper = Mappers.getMapper(ShopMapper.class);
        Shop shop = shopRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<com.sang.sourcepattern.entity.Pet> pets = petRepository.findByOwnerId(customerId);
        List<Booking> userBookings = bookingRepository.findByShopId(shop.getId()).stream()
                .filter(b -> b.getUser().getId() == customerId)
                .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                .collect(java.util.stream.Collectors.toList());

        // Recalculate summary stats for this user
        java.math.BigDecimal totalSpent = userBookings.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .map(b -> b.getServices() != null ? b.getServices().stream().map(s -> s.getPrice() != null ? s.getPrice() : java.math.BigDecimal.ZERO).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.time.LocalDateTime lastVisit = userBookings.stream()
                .map(Booking::getAppointmentDatetime)
                .filter(java.util.Objects::nonNull)
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null);

        String tier = "NEW";
        if (totalSpent.compareTo(new java.math.BigDecimal("5000000")) >= 0 || userBookings.size() >= 20) {
            tier = "VIP";
        } else if (userBookings.size() >= 10) {
            tier = "REGULAR";
        }

        CustomerItemResponse info = CustomerItemResponse.builder()
                .id(customer.getId())
                .name(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .avatar(customer.getAvatar())
                .pets(pets.size())
                .totalBookings(userBookings.size())
                .totalSpent(String.format("%,.0fđ", totalSpent.doubleValue()))
                .lastVisit(lastVisit != null ? lastVisit.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A")
                .tier(tier)
                .build();

        return CustomerDetailResponse.builder()
                .customerInfo(info)
                .pets(pets.stream().map(petMapper::toPetResponse).collect(java.util.stream.Collectors.toList()))
                .bookingHistory(userBookings.stream().map(bookingMapper::toBookingResponse).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    @Override
    public ShopCustomerResponse getShopCustomers(String ownerEmail) {
        Shop shop = shopRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        List<User> customers = userRepository.findUsersByShopId(shop.getId());
        List<Booking> allShopBookings = bookingRepository.findByShopId(shop.getId());

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        List<CustomerItemResponse> customerList = customers.stream().map(user -> {
            List<Booking> userBookings = allShopBookings.stream()
                    .filter(b -> b.getUser().getId() == user.getId())
                    .collect(java.util.stream.Collectors.toList());

            java.math.BigDecimal totalSpent = userBookings.stream()
                    .filter(b -> "COMPLETED".equals(b.getStatus()))
                    .map(b -> b.getServices() != null ? b.getServices().stream().map(s -> s.getPrice() != null ? s.getPrice() : java.math.BigDecimal.ZERO).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            long petCount = petRepository.countByOwnerId(user.getId());
            
            java.time.LocalDateTime lastVisit = userBookings.stream()
                    .map(Booking::getAppointmentDatetime)
                    .filter(java.util.Objects::nonNull)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null);

            // Determine Tier logic
            String tier = "NEW";
            if (totalSpent.compareTo(new java.math.BigDecimal("5000000")) >= 0 || userBookings.size() >= 20) {
                tier = "VIP";
            } else if (userBookings.size() >= 10) {
                tier = "REGULAR";
            }

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

            return CustomerItemResponse.builder()
                    .id(user.getId())
                    .name(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .avatar(user.getAvatar())
                    .pets((int) petCount)
                    .totalBookings(userBookings.size())
                    .totalSpent(String.format("%,.0fđ", totalSpent.doubleValue()))
                    .lastVisit(lastVisit != null ? lastVisit.format(formatter) : "N/A")
                    .tier(tier)
                    .build();
        }).collect(java.util.stream.Collectors.toList());

        long totalCustomers = customerList.size();
        
        // A customer is "new" if their FIRST booking at this shop was this month
        long newCustomers = customers.stream().filter(user -> {
            return allShopBookings.stream()
                    .filter(b -> b.getUser().getId() == user.getId())
                    .map(Booking::getCreatedAt)
                    .min(java.time.LocalDateTime::compareTo)
                    .map(firstDate -> firstDate.isAfter(startOfMonth))
                    .orElse(false);
        }).count();

        long loyalCustomers = customerList.stream()
                .filter(c -> "VIP".equals(c.getTier()) || "REGULAR".equals(c.getTier()))
                .count();

        return ShopCustomerResponse.builder()
                .totalCustomers(totalCustomers)
                .newCustomersThisMonth(newCustomers)
                .loyalCustomers(loyalCustomers)
                .customers(customerList)
                .build();
    }

    private java.math.BigDecimal calculateShopRevenueForBooking(Booking b) {
        if (b.getServices() == null || b.getServices().isEmpty()) return java.math.BigDecimal.ZERO;
        return b.getServices().stream().map(s -> {
            if (s.getPrice() == null) return java.math.BigDecimal.ZERO;
            String cat = s.getCategory() != null ? s.getCategory().toLowerCase() : "";
            double multiplier = 0.90;
            if (cat.contains("grooming") || cat.contains("spa")) multiplier = 0.82;
            else if ((cat.contains("boarding") || cat.contains("hotel")) && s.isCameraEnabled()) multiplier = 0.75;
            return s.getPrice().multiply(new java.math.BigDecimal(String.valueOf(multiplier)));
        }).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    @Override
    public ShopDashboardResponse getShopDashboard(String ownerEmail, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        Shop shop = shopRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        List<Booking> allBookings = bookingRepository.findByShopId(shop.getId());
        List<com.sang.sourcepattern.entity.Transaction> shopTxns = transactionRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // Default to start of month to now if not provided
        java.time.LocalDateTime start = startDate != null ? startDate.atStartOfDay() : now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        java.time.LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : now;
        
        if (start.isAfter(end)) {
            java.time.LocalDateTime temp = start;
            start = end;
            end = temp;
        }

        // 1. All-time stats (totalRevenue, totalBookings, totalCustomers, totalPets)
        java.math.BigDecimal totalRefunds = shopTxns.stream()
                .filter(t -> "REFUND".equals(t.getType()) && "SUCCESS".equals(t.getStatus()))
                .map(com.sang.sourcepattern.entity.Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .map(this::calculateShopRevenueForBooking)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .subtract(totalRefunds);

        long totalBookings = allBookings.size();
        long pendingBookings = allBookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus()) || "PENDING_PAYMENT".equals(b.getStatus()))
                .count();

        long totalCustomers = allBookings.stream()
                .map(b -> b.getUser().getId())
                .distinct()
                .count();

        long totalPets = allBookings.stream()
                .map(b -> b.getPet().getId())
                .distinct()
                .count();

        // 2. Period stats
        final java.time.LocalDateTime finalStart = start;
        final java.time.LocalDateTime finalEnd = end;
        
        List<Booking> periodBookingsList = allBookings.stream()
                .filter(b -> b.getCreatedAt().isAfter(finalStart) && b.getCreatedAt().isBefore(finalEnd))
                .collect(java.util.stream.Collectors.toList());
                
        long periodBookingsCount = periodBookingsList.size();
        
        long periodNewCustomers = periodBookingsList.stream()
                .map(b -> b.getUser().getId())
                .distinct()
                .filter(userId -> {
                    return allBookings.stream()
                            .filter(ab -> ab.getUser().getId() == userId)
                            .map(Booking::getCreatedAt)
                            .min(java.time.LocalDateTime::compareTo)
                            .map(firstDate -> firstDate.isAfter(finalStart) && firstDate.isBefore(finalEnd))
                            .orElse(false);
                })
                .count();

        java.math.BigDecimal refundsInPeriod = shopTxns.stream()
                .filter(t -> "REFUND".equals(t.getType()) && "SUCCESS".equals(t.getStatus()))
                .filter(t -> t.getCreatedAt().isAfter(finalStart) && t.getCreatedAt().isBefore(finalEnd))
                .map(com.sang.sourcepattern.entity.Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal revenueInPeriod = periodBookingsList.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .map(this::calculateShopRevenueForBooking)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .subtract(refundsInPeriod);

        long periodPending = periodBookingsList.stream().filter(b -> "PENDING_PAYMENT".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())).count();
        long periodCompleted = periodBookingsList.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count();
        long periodCancelled = periodBookingsList.stream().filter(b -> "CANCELLED".equals(b.getStatus()) || "NO_SHOW".equals(b.getStatus()) || "REJECTED".equals(b.getStatus())).count();
        ShopDashboardResponse.BookingStatusStats statusStats = new ShopDashboardResponse.BookingStatusStats(periodPending, periodPending, periodCompleted, periodCancelled);

        // 3. Revenue Chart (Dynamic based on period up to 60 days, else month)
        java.time.format.DateTimeFormatter dayFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
        List<ShopDashboardResponse.RevenueChartData> revenueChart = new java.util.ArrayList<>();
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        if (daysBetween > 60) {
            java.time.format.DateTimeFormatter monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MM/yyyy");
            java.time.LocalDate currentMonthStart = start.toLocalDate().withDayOfMonth(1);
            while (!currentMonthStart.isAfter(end.toLocalDate().withDayOfMonth(1))) {
                final java.time.LocalDate loopMonth = currentMonthStart;
                java.math.BigDecimal monthAmount = allBookings.stream()
                        .filter(b -> "COMPLETED".equals(b.getStatus()))
                        .filter(b -> b.getAppointmentDatetime() != null && b.getAppointmentDatetime().toLocalDate().withDayOfMonth(1).equals(loopMonth))
                        .map(this::calculateShopRevenueForBooking)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
                java.math.BigDecimal monthRefunds = shopTxns.stream()
                        .filter(t -> "REFUND".equals(t.getType()) && "SUCCESS".equals(t.getStatus()))
                        .filter(t -> t.getCreatedAt().toLocalDate().withDayOfMonth(1).equals(loopMonth))
                        .map(com.sang.sourcepattern.entity.Transaction::getAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
                revenueChart.add(new ShopDashboardResponse.RevenueChartData(loopMonth.format(monthFormatter), monthAmount.subtract(monthRefunds)));
                currentMonthStart = currentMonthStart.plusMonths(1);
            }
        } else {
            java.time.LocalDate currentDate = start.toLocalDate();
            while (!currentDate.isAfter(end.toLocalDate())) {
                final java.time.LocalDate loopDate = currentDate;
                java.math.BigDecimal dayAmount = allBookings.stream()
                        .filter(b -> "COMPLETED".equals(b.getStatus()))
                        .filter(b -> b.getAppointmentDatetime() != null && b.getAppointmentDatetime().toLocalDate().equals(loopDate))
                        .map(this::calculateShopRevenueForBooking)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
                java.math.BigDecimal dayRefunds = shopTxns.stream()
                        .filter(t -> "REFUND".equals(t.getType()) && "SUCCESS".equals(t.getStatus()))
                        .filter(t -> t.getCreatedAt().toLocalDate().equals(loopDate))
                        .map(com.sang.sourcepattern.entity.Transaction::getAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
                revenueChart.add(new ShopDashboardResponse.RevenueChartData(loopDate.format(dayFormatter), dayAmount.subtract(dayRefunds)));
                currentDate = currentDate.plusDays(1);
            }
        }

        // 4. Top Services in Period
        java.util.Map<String, Long> serviceCounts = periodBookingsList.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        b -> b.getServices() != null && !b.getServices().isEmpty() ? b.getServices().iterator().next().getServiceName() : "",
                        java.util.stream.Collectors.counting()
                ));

        List<ShopDashboardResponse.ServiceStat> topServices = serviceCounts.entrySet().stream()
                .filter(e -> !e.getKey().isEmpty())
                .map(e -> new ShopDashboardResponse.ServiceStat(e.getKey(), e.getValue()))
                .sorted((s1, s2) -> Long.compare(s2.getCount(), s1.getCount()))
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        // 5. Growth compared to previous period
        long periodDurationSeconds = java.time.temporal.ChronoUnit.SECONDS.between(start, end);
        java.time.LocalDateTime prevStart = start.minusSeconds(periodDurationSeconds);
        java.time.LocalDateTime prevEnd = start;

        java.math.BigDecimal refundsPrevPeriod = shopTxns.stream()
                .filter(t -> "REFUND".equals(t.getType()) && "SUCCESS".equals(t.getStatus()))
                .filter(t -> t.getCreatedAt().isAfter(prevStart) && t.getCreatedAt().isBefore(prevEnd))
                .map(com.sang.sourcepattern.entity.Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal revenuePrevPeriod = allBookings.stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .filter(b -> b.getAppointmentDatetime() != null && b.getAppointmentDatetime().isAfter(prevStart) && b.getAppointmentDatetime().isBefore(prevEnd))
                .map(this::calculateShopRevenueForBooking)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .subtract(refundsPrevPeriod);

        Double growthPercentage = 0.0;
        if (revenuePrevPeriod.compareTo(java.math.BigDecimal.ZERO) > 0) {
            growthPercentage = revenueInPeriod.subtract(revenuePrevPeriod)
                    .divide(revenuePrevPeriod, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new java.math.BigDecimal("100"))
                    .doubleValue();
        } else if (revenueInPeriod.compareTo(java.math.BigDecimal.ZERO) > 0) {
            growthPercentage = 100.0;
        }

        String topServiceName = topServices.isEmpty() ? "các dịch vụ" : topServices.get(0).getName();
        String growthDescription = growthPercentage >= 0 
            ? "Hệ thống ghi nhận sự tăng trưởng ổn định so với kỳ trước nhờ vào " + topServiceName + "."
            : "Doanh thu đang giảm so với kỳ trước, cân nhắc tạo mã giảm giá cho " + topServiceName + ".";

        return ShopDashboardResponse.builder()
                .totalRevenue(totalRevenue)
                .periodRevenue(revenueInPeriod)
                .totalBookings(totalBookings)
                .pendingBookings(pendingBookings)
                .totalCustomers(totalCustomers)
                .totalPets(totalPets)
                .periodBookings(periodBookingsCount)
                .periodNewCustomers(periodNewCustomers)
                .revenueChart(revenueChart)
                .topServices(topServices)
                .bookingStatusStats(statusStats)
                .growthPercentage(growthPercentage)
                .growthDescription(growthDescription)
                .build();
    }

    @Override
    @Transactional
    public ShopResponse registerShop(ShopRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (shopRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.SHOP_EXISTED);
        }

        // 1. Create SHOP_OWNER user
        Role shopOwnerRole = roleRepository.findByName("SHOP_OWNER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<Role> roles = new HashSet<>();
        roles.add(shopOwnerRole);

        User owner = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getShopName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .roles(roles)
                .active(false)
                .emailVerified(false)
                .build();

        owner = userRepository.save(owner);

        // 2. Create Shop record
        Shop shop = shopMapper.toShop(request);
        shop.setOwner(owner);
        shop.setVerified(false);
        shop.setStatus(ShopStatus.PENDING);

        // 2.1. Auto-geocode address to get lat/lng
        if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            LatLong location = goongMapService.geocodeAddress(request.getAddress());
            if (location != null) {
                shop.setLatitude(location.getLatitude());
                shop.setLongitude(location.getLongitude());
            }
        }

        shop = shopRepository.save(shop);

        // 3. Tạo OTP và flush trước khi gửi email
        String otp = String.format("%06d", new java.util.Random().nextInt(1_000_000));
        userTokenRepository.saveAndFlush(
                com.sang.sourcepattern.entity.UserToken.builder()
                        .token(otp)
                        .type("VERIFY_EMAIL")
                        .user(owner)
                        .expiresAt(java.time.LocalDateTime.now().plusMinutes(10))
                        .build()
        );

        // 4. Gửi email OTP (async — không block response)
        emailService.sendVerificationEmail(owner.getEmail(), owner.getFullName(), otp);

        log.info("Shop registered successfully: {} (Pending Approval)", shop.getShopName());

        ShopResponse response = shopMapper.toShopResponse(shop);
        response.setStaffs(getStaffByShop(response.getId()));
        return response;
    }

    @Override
    @Transactional
    public ShopResponse approveShop(int shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        shop.setVerified(true);
        shop.setStatus(ShopStatus.APPROVED);
        shopRepository.save(shop);

        // Kích hoạt tài khoản owner
        User owner = shop.getOwner();
        owner.setActive(true);
        userRepository.save(owner);

        // Gửi email thông báo duyệt
        emailService.sendShopApprovedEmail(shop.getEmail(), shop.getShopName());

        log.info("Shop approved by admin: {}", shop.getShopName());

        ShopResponse response = shopMapper.toShopResponse(shop);
        response.setStaffs(getStaffByShop(response.getId()));
        return response;
    }

    @Override
    public List<ShopResponse> getAllShops() {
        return shopRepository.findAll().stream()
                .map(shopMapper::toShopResponse)
                .toList();
    }

    @Override
    public ShopResponse getShopById(int id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        ShopResponse response = shopMapper.toShopResponse(shop);
        response.setStaffs(getStaffByShop(id));
        return response;
    }

    @Override
    public ShopResponse getMyShop(String email) {
        Shop shop = shopRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        ShopResponse response = shopMapper.toShopResponse(shop);
        response.setStaffs(getStaffByShop(response.getId()));
        return response;
    }

    @Override
    @Transactional
    public ShopResponse updateMyShop(String email, ShopUpdateRequest request) {
        log.info("Updating shop profile for owner: {}", email);
        log.info("Request data: {}", request);
        
        Shop shop = shopRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        String oldOffDays = shop.getOffDays();

        shopMapper.updateShop(shop, request);

        String newOffDays = shop.getOffDays();
        if (newOffDays != null && !newOffDays.equals(oldOffDays)) {
            // Find all ADMINs and notify
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName())))
                    .collect(java.util.stream.Collectors.toList());
            for (User admin : admins) {
                notificationRepository.save(com.sang.sourcepattern.entity.Notification.builder()
                        .user(admin)
                        .title("Cập nhật ngày nghỉ (Shop Off)")
                        .content("Shop '" + shop.getShopName() + "' đã cập nhật ngày nghỉ: " + newOffDays)
                        .notificationType(com.sang.sourcepattern.entity.Notification.NotificationType.SYSTEM)
                        .build());
            }
            log.info("Sent notification to admins about offDays update for shop {}", shop.getShopName());
        }

        // Validate & apply lateGracePeriod nếu có truyền lên
        if (request.getLateGracePeriod() != null) {
            if (request.getLateGracePeriod() < 5 || request.getLateGracePeriod() > 30) {
                throw new AppException(ErrorCode.INVALID_GRACE_PERIOD);
            }
            shop.setLateGracePeriod(request.getLateGracePeriod());
        }
        
        // Re-geocode if address changed
        if (request.getAddress() != null && !request.getAddress().isEmpty()) {
            LatLong location = goongMapService.geocodeAddress(request.getAddress());
            if (location != null) {
                shop.setLatitude(location.getLatitude());
                shop.setLongitude(location.getLongitude());
            }
        }
        
        log.info("Shop entity after mapping: Logo={}, Banner={}", shop.getLogoUrl(), shop.getBannerUrl());
        
        Shop saved = shopRepository.save(shop);
        ShopResponse response = shopMapper.toShopResponse(saved);
        response.setStaffs(getStaffByShop(response.getId()));
        return response;
    }

    @Override
    public List<ShopResponse> searchVerifiedShops(String keyword, String city, String shopType) {
        return shopRepository.searchVerified(keyword, city, shopType)
                .stream()
                .map(shopMapper::toShopResponse)
                .toList();
    }

    @Override
    public ShopResponse getVerifiedShopById(int id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        if (!shop.isVerified()) {
            throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        }
        ShopResponse response = shopMapper.toShopResponse(shop);
        response.setStaffs(getStaffByShop(response.getId()));
        return response;
    }

    @Override
    @Transactional
    public void rejectShop(int shopId, String reason) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        User owner = shop.getOwner();

        // Gửi email từ chối trước khi xóa data
        emailService.sendShopRejectedEmail(shop.getEmail(), shop.getShopName(), reason);

        // Shop mới PENDING chỉ có: user_token → shop → user
        // Xóa theo đúng thứ tự FK
        if (owner != null) {
            userTokenRepository.deleteByUserId(owner.getId());
        }
        shopRepository.delete(shop);

        if (owner != null && !owner.isActive()) {
            userRepository.delete(owner);
        }

        log.info("Shop registration rejected and deleted: {}", shop.getShopName());
    }

    @Override
    public List<ShopResponse> getPendingShops() {
        return shopRepository.findAll().stream()
                .filter(s -> s.getStatus() == ShopStatus.PENDING)
                .map(shopMapper::toShopResponse)
                .toList();
    }

    @Override
    public List<StaffResponse> getStaffByShop(int shopId) {
        shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return staffRepository.findByShopId(shopId).stream()
                .map(s -> {
                    List<StaffCertificateResponse> certs = staffCertificateRepository.findByStaffId(s.getId()).stream()
                            .map(c -> StaffCertificateResponse.builder()
                                    .id(c.getId())
                                    .certificateName(c.getCertificateName())
                                    .imageUrl(c.getImageUrl())
                                    .issueDate(c.getIssueDate())
                                    .expiryDate(c.getExpiryDate())
                                    .status(c.getStatus())
                                    .build())
                            .toList();

                    return StaffResponse.builder()
                            .id(s.getId())
                            .shopId(shopId)
                            .userId(s.getUser() != null ? s.getUser().getId() : null)
                            .email(s.getUser() != null ? s.getUser().getEmail() : null)
                            .fullName(s.getFullName())
                            .role(s.getRole())
                            .phone(s.getPhone())
                            .specialization(s.getSpecialization())
                            .avatar(s.getUser() != null ? s.getUser().getAvatar() : null)
                            .isActive(s.isActive())
                            .certificates(certs)
                            .build();
                })
                .toList();
    }

    @Override
    public List<com.sang.sourcepattern.dto.response.ShopNearbyResponse> searchNearbyShops(
            Double latitude, Double longitude, Double radiusKm) {
        
        LatLong userLocation = new LatLong(latitude, longitude);
        
        // Get all verified shops
        List<Shop> verifiedShops = shopRepository.findAll().stream()
                .filter(Shop::isVerified)
                .toList();
        
        // Calculate distance for each shop and filter by radius
        return verifiedShops.stream()
                .map(shop -> {
                    if (shop.getLatitude() == null || shop.getLongitude() == null) {
                        return null; // Skip shops without coordinates
                    }
                    
                    LatLong shopLocation =
                            new LatLong(shop.getLatitude(), shop.getLongitude());
                    
                    double distanceKm = goongMapService.getDistanceKm(userLocation, shopLocation);
                    
                    if (distanceKm == Double.MAX_VALUE || distanceKm > radiusKm) {
                        return null; // Skip if distance calculation failed or out of radius
                    }
                    
                    return com.sang.sourcepattern.dto.response.ShopNearbyResponse.builder()
                            .id(shop.getId())
                            .shopName(shop.getShopName())
                            .shopType(shop.getShopType())
                            .address(shop.getAddress())
                            .city(shop.getCity())
                            .latitude(shop.getLatitude())
                            .longitude(shop.getLongitude())
                            .logoUrl(shop.getLogoUrl())
                            .ratingAvg(shop.getRatingAvg())
                            .distanceKm(Math.round(distanceKm * 10.0) / 10.0) // Round to 1 decimal
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(com.sang.sourcepattern.dto.response.ShopNearbyResponse::getDistanceKm))
                .toList();
    }

    @Override
    public com.sang.sourcepattern.dto.response.goong.GoongDirectionsResponse getDirectionsToShop(
            int shopId, Double fromLat, Double fromLng) {
        
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        
        if (!shop.isVerified()) {
            throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        }
        
        if (shop.getLatitude() == null || shop.getLongitude() == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        
        LatLong origin = new LatLong(fromLat, fromLng);
        LatLong destination =
                new LatLong(shop.getLatitude(), shop.getLongitude());
        
        return goongMapService.getDirections(origin, destination);
    }
}
