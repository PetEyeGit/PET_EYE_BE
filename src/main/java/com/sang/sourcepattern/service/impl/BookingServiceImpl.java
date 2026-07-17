package com.sang.sourcepattern.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sang.sourcepattern.dto.request.BookingCreationRequest;
import com.sang.sourcepattern.dto.request.InitiatePaymentRequest;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.InitiatePaymentResponse;
import com.sang.sourcepattern.dto.response.PayOSCreateResponse;
import com.sang.sourcepattern.dto.response.PayOSPaymentInfoResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.entity.*;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.*;
import com.sang.sourcepattern.service.BookingService;
import com.sang.sourcepattern.service.PayOSService;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingServiceImpl implements BookingService {

    BookingRepository     bookingRepository;
    PaymentRepository     paymentRepository;
    TransactionRepository transactionRepository;
    UserRepository        userRepository;
    ShopRepository        shopRepository;
    ServiceRepository     serviceRepository;
    PetRepository         petRepository;
    StaffRepository       staffRepository;
    PayOSService          payOSService;
    WalletServiceImpl     walletService;
    NotificationRepository notificationRepository;
    com.sang.sourcepattern.service.CameraService cameraService;
    UserVoucherRepository userVoucherRepository;
    com.sang.sourcepattern.service.EmailService emailService;
    /** Redis template để lưu PendingBooking thay vì in-memory */
    RedisTemplate<String, Object> redisTemplate;
    SimpMessagingTemplate messagingTemplate;

    @Value("${payos.return-url}") @NonFinal String returnUrl;
    @Value("${payos.cancel-url}") @NonFinal String cancelUrl;

    /** TTL cho pending booking trong Redis: 30 phút */
    private static final long PENDING_TTL_MINUTES = 30;
    private static final String REDIS_KEY_PREFIX   = "pending_booking:";

    /**
     * Redis key prefix cho cash deposit pending booking.
     * Format: cash_pending:{orderCode}
     */
    private static final String CASH_PENDING_PREFIX = "cash_pending:";

    /**
     * Redis lock key cho staff slot.
     * Format: staff_slot:{staffId}:{yyyy-MM-ddTHH:mm}
     * TTL = 30 phút (hết hạn cùng pending booking)
     */
    private static final String STAFF_SLOT_PREFIX  = "staff_slot:";

    // ─── PendingBooking DTO (lưu vào Redis) ──────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PendingBooking implements Serializable {
        int userId;
        int shopId;
        int serviceId;
        /** Danh sách tất cả service IDs (multi-service support) */
        java.util.List<Integer> serviceIds;
        int petId;
        Integer staffId;
        LocalDateTime appointmentDatetime;
        LocalDateTime checkIn;
        LocalDateTime checkOut;
        String note;
        int amountVnd;
        String description;
        LocalDateTime checkOutDatetime;
        String petWeight;
        String roomType;
        Integer userVoucherId;
        Integer updateBookingId;
    }

    // ─── Redis helpers ────────────────────────────────────────────────────────

    private String redisKey(long orderCode) {
        return REDIS_KEY_PREFIX + orderCode;
    }

    private void savePending(long orderCode, PendingBooking pending) {
        redisTemplate.opsForValue().set(redisKey(orderCode), pending, PENDING_TTL_MINUTES, TimeUnit.MINUTES);
    }

    private PendingBooking getPending(long orderCode) {
        Object raw = redisTemplate.opsForValue().get(redisKey(orderCode));
        if (raw == null) return null;
        if (raw instanceof PendingBooking pb) return pb;
        // Fallback: Jackson deserialization via RedisTemplate
        return null;
    }

    private void deletePending(long orderCode) {
        redisTemplate.delete(redisKey(orderCode));
    }

    // ─── Staff slot lock (Redis) ──────────────────────────────────────────────

    /**
     * Key: staff_slot:{staffId}:{appointmentTime truncated to minute}
     * Value: orderCode (để biết ai đang giữ slot)
     */
    private String staffSlotKey(int staffId, LocalDateTime appointmentTime) {
        // Truncate to minute để tránh mismatch giây
        String timeStr = appointmentTime.withSecond(0).withNano(0).toString();
        return STAFF_SLOT_PREFIX + staffId + ":" + timeStr;
    }

    /**
     * Thử giữ slot của staff bằng Redis SET NX (atomic).
     * @return true nếu giữ thành công, false nếu slot đã bị giữ
     */
    private boolean tryLockStaffSlot(int staffId, LocalDateTime appointmentTime, long orderCode) {
        String key = staffSlotKey(staffId, appointmentTime);
        Boolean set = redisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(orderCode), PENDING_TTL_MINUTES, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(set);
    }

    /** Giải phóng slot của staff khi booking bị huỷ hoặc confirm xong */
    private void releaseStaffSlot(int staffId, LocalDateTime appointmentTime) {
        redisTemplate.delete(staffSlotKey(staffId, appointmentTime));
    }

    // ─── General helpers ──────────────────────────────────────────────────────

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private BookingResponse toResponse(Booking booking) {
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);

        // Check if current user is owner or staff of the shop
        boolean isShopUser = false;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                String email = jwt.getClaim("email");
                if (booking.getShop().getOwner().getEmail().equalsIgnoreCase(email)) {
                    isShopUser = true;
                } else {
                    isShopUser = staffRepository.findByUserEmail(email)
                            .map(s -> s.getShop().getId() == booking.getShop().getId())
                            .orElse(false);
                }
            }
        } catch (Exception e) {
            // ignore
        }

        boolean isAccepted = java.util.List.of("CONFIRMED", "IN_PROGRESS", "COMPLETED").contains(booking.getStatus());

        // Primary service = service dau tien trong Set (tuong thich nguoc)
        Service primaryService = (booking.getServices() != null && !booking.getServices().isEmpty())
                ? booking.getServices().iterator().next() : null;

        // Tong gia = cong tat ca services
        java.math.BigDecimal totalServicePrice = (booking.getServices() != null)
                ? booking.getServices().stream()
                        .map(s -> {
                            java.math.BigDecimal price = walletService.resolveSingleServicePrice(s, booking.getPet() != null ? booking.getPet().getId() : null, booking.getPetWeight());
                            return price != null ? price : java.math.BigDecimal.ZERO;
                        })
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                : java.math.BigDecimal.ZERO;

        // Check category tu bat ky service nao trong list
        boolean isLodging = booking.getServices() != null && booking.getServices().stream()
                .anyMatch(s -> s.getCategory() != null
                        && (s.getCategory().equalsIgnoreCase("BOARDING") || s.getCategory().equalsIgnoreCase("Hotel")));
        boolean cameraEnabled = isLodging && booking.getServices() != null
                && booking.getServices().stream().anyMatch(Service::isCameraEnabled);
        String streamUrl = (isAccepted && cameraEnabled) ? booking.getCameraStreamUrl() : null;

        // Duration tong de tinh serviceEndDatetime
        int totalDuration = (booking.getServices() != null)
                ? booking.getServices().stream().mapToInt(s -> s.getDurationMinutes() != 0 ? s.getDurationMinutes() : 60).sum()
                : 60;

        // Danh sach serviceDtos
        java.util.List<BookingResponse.BookingServiceDto> serviceDtos = (booking.getServices() != null)
                ? booking.getServices().stream().map(s -> BookingResponse.BookingServiceDto.builder()
                        .serviceId(s.getId())
                        .serviceName(s.getServiceName())
                        .servicePrice(walletService.resolveSingleServicePrice(s, booking.getPet() != null ? booking.getPet().getId() : null, booking.getPetWeight()))
                        .category(s.getCategory())
                        .durationMinutes(s.getDurationMinutes())
                        .build()).collect(Collectors.toList())
                : new java.util.ArrayList<>();

        java.math.BigDecimal paidAmount = java.math.BigDecimal.ZERO;
        if (payment != null && "SUCCESS".equals(payment.getStatus())) {
            paidAmount = payment.getAmount();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .shopId(booking.getShop().getId())
                .shopName(booking.getShop().getShopName())
                .shopAddress(booking.getShop().getAddress())
                .serviceId(primaryService != null ? primaryService.getId() : 0)
                .serviceName(primaryService != null ? primaryService.getServiceName() : "")
                .servicePrice(totalServicePrice)
                .services(serviceDtos)
                .petId(booking.getPet().getId())
                .petName(booking.getPet().getName())
                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhone() : null)
                .customerAddress(booking.getUser() != null ? booking.getUser().getAddress() : null)
                .staffId(booking.getStaff() != null ? booking.getStaff().getId() : null)
                .staffName(booking.getStaff() != null ? booking.getStaff().getFullName() : null)
                .appointmentDatetime(booking.getAppointmentDatetime())
                .status(booking.getStatus())
                .note(booking.getNote())
                .cancellationReason(booking.getCancellationReason())
                .bankName(booking.getBankName())
                .bankAccount(booking.getBankAccount())
                .accountHolder(booking.getAccountHolder())
                .payosOrderCode(booking.getPayosOrderCode())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .checkoutUrl(payment != null ? payment.getCheckoutUrl() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .paidAmount(paidAmount)
                .cameraRtspUrl(isShopUser ? booking.getCameraRtspUrl() : null)
                .cameraStreamUrl(streamUrl)
                .cameraEnabled(cameraEnabled)
                .cameraConfiguredAt(booking.getCameraConfiguredAt())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .serviceEndDatetime(isLodging && booking.getCheckOut() != null
                        ? booking.getCheckOut()
                        : (booking.getAppointmentDatetime() != null
                                ? booking.getAppointmentDatetime().plusMinutes(totalDuration)
                                : null))
                .petWeight(booking.getPetWeight())
                .roomType(booking.getRoomType())
                .category(isLodging ? "BOARDING" : (primaryService != null ? primaryService.getCategory() : null))
                .build();
    }

    /**
     * Kiểm tra pet có booking trùng giờ không.
     * Dùng interval overlap: newStart < existingEnd AND existingStart < newEnd
     * windowStart = appointmentTime (bắt đầu booking mới)
     * windowEnd   = appointmentTime + durationMinutes (kết thúc booking mới)
     */
    private void checkPetConflict(int petId, int shopId, LocalDateTime appointmentTime, int durationMinutes, boolean isBoarding) {
        LocalDateTime windowStart = appointmentTime;
        LocalDateTime windowEnd   = appointmentTime.plusMinutes(durationMinutes);

        boolean conflict = bookingRepository.countConflictingBookingForPet(
                petId, windowStart, windowEnd, shopId, isBoarding) > 0;

        if (conflict) {
            throw new AppException(ErrorCode.PET_BOOKING_CONFLICT);
        }
    }

    /**
     * Kiểm tra staff có bị trùng lịch không.
     * Dùng interval overlap: newStart < existingEnd AND existingStart < newEnd
     */
    private void checkStaffConflict(int staffId, LocalDateTime appointmentTime, int durationMinutes, boolean isBoarding) {
        // Không bypass khi isBoarding — query SQL đã tự loại trừ boarding/hotel
        LocalDateTime windowStart = appointmentTime;
        LocalDateTime windowEnd   = appointmentTime.plusMinutes(durationMinutes);

        boolean dbConflict = bookingRepository.countConflictingBookingForStaff(
                staffId, windowStart, windowEnd) > 0;
        if (dbConflict) {
            throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
        }

        // Kiểm tra Redis — pending booking đang giữ slot (chưa thanh toán)
        String slotKey = staffSlotKey(staffId, appointmentTime);
        Boolean slotTaken = redisTemplate.hasKey(slotKey);
        if (Boolean.TRUE.equals(slotTaken)) {
            throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
        }
    }

    /**
     * Kiểm tra shop có ít nhất 1 staff active còn rảnh trong khung giờ đó không.
     * Nếu không có ai rảnh → throw NO_STAFF_AVAILABLE.
     * Chỉ gọi khi user KHÔNG chọn staff cụ thể (staffId == null).
     */
    private void checkShopHasAvailableStaff(int shopId, LocalDateTime appointmentTime, int durationMinutes, boolean isBoarding) {
        List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shopId);
        if (activeStaff.isEmpty()) {
            throw new AppException(ErrorCode.NO_STAFF_AVAILABLE);
        }
        // Không bypass khi isBoarding — query SQL đã tự loại trừ boarding/hotel

        LocalDateTime windowStart = appointmentTime;
        LocalDateTime windowEnd   = appointmentTime.plusMinutes(durationMinutes);

        boolean anyFree = activeStaff.stream().anyMatch(s -> {
            boolean dbBusy = bookingRepository.countConflictingBookingForStaff(
                    s.getId(), windowStart, windowEnd) > 0;
            if (dbBusy) return false;
            String slotKey = staffSlotKey(s.getId(), appointmentTime);
            return !Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
        });

        if (!anyFree) {
            throw new AppException(ErrorCode.NO_STAFF_AVAILABLE);
        }
    }

    // ─── Multi-service helpers ────────────────────────────────────────────────

    /**
     * Trả về danh sách serviceIds hiệu quả:
     * - Nếu serviceIds không null/empty → dùng serviceIds
     * - Ngược lại → dùng [serviceId]
     */
    private List<Integer> resolveServiceIds(Integer serviceId, List<Integer> serviceIds) {
        if (serviceIds != null && !serviceIds.isEmpty()) return serviceIds;
        return java.util.Collections.singletonList(serviceId);
    }

    private java.math.BigDecimal resolveSingleServicePrice(Service s, Integer petId, String petWeight) {
        java.math.BigDecimal price = s.getPrice();
        if (s.getPetWeight() != null) {
            try {
                List<String> weightTiers = new com.fasterxml.jackson.databind.ObjectMapper().readValue(s.getPetWeight(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                List<java.math.BigDecimal> prices = null;
                if (s.getPrices() != null) {
                    prices = new com.fasterxml.jackson.databind.ObjectMapper().readValue(s.getPrices(), new com.fasterxml.jackson.core.type.TypeReference<List<java.math.BigDecimal>>() {});
                }
                if (weightTiers != null && !weightTiers.isEmpty() && prices != null && !prices.isEmpty()) {
                    Double weight = null;
                    if (petId != null) {
                        Pet pet = petRepository.findById(petId).orElse(null);
                        if (pet != null) {
                            weight = (double) pet.getWeight();
                        }
                    }
                    if (weight == null || weight <= 0) {
                        if (petWeight != null) {
                            try {
                                weight = Double.parseDouble(petWeight.replaceAll("[^0-9.]", ""));
                            } catch (Exception ignored) {}
                        }
                    }
                    if (weight != null && weight > 0) {
                        List<Double> thresholds = new java.util.ArrayList<>();
                        for (String tier : weightTiers) {
                            try {
                                thresholds.add(Double.parseDouble(tier.replaceAll("[^0-9.]", "")));
                            } catch (Exception e) {
                                thresholds.add(0.0);
                            }
                        }
                        int idx = 0;
                        if (weight < thresholds.get(0)) {
                            idx = 0;
                        } else {
                            for (int i = 0; i < thresholds.size(); i++) {
                                if (weight >= thresholds.get(i)) {
                                    idx = i;
                                }
                            }
                        }
                        if (idx < prices.size() && prices.get(idx) != null) {
                            price = prices.get(idx);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return price != null ? price : java.math.BigDecimal.ZERO;
    }

    /**
     * Tính tổng giá của danh sách services.
     */
    private java.math.BigDecimal resolveTotalPrice(List<Integer> ids, Integer petId, String petWeight) {
        return ids.stream()
                .map(id -> serviceRepository.findById(id)
                        .map(s -> resolveSingleServicePrice(s, petId, petWeight))
                        .orElse(java.math.BigDecimal.ZERO))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    /**
     * Tính tổng duration (phút) của danh sách services.
     */
    private int resolveTotalDuration(List<Integer> ids) {
        int total = ids.stream()
                .mapToInt(id -> serviceRepository.findById(id)
                        .map(Service::getDurationMinutes)
                        .orElse(0))
                .sum();
        return total > 0 ? total : 60;
    }

    private void validateBookingInputs(int shopId, int serviceId, int petId,
                                        Integer staffId, int userId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        if (!shop.isVerified()) throw new AppException(ErrorCode.SHOP_NOT_VERIFIED);

        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_NOT_FOUND));
        if (service.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.SERVICE_NOT_BELONG_TO_SHOP);

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
        if (pet.getOwner().getId() != userId)
            throw new AppException(ErrorCode.PET_NOT_BELONG_TO_USER);

        if (staffId != null) {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            if (staff.getShop().getId() != shopId)
                throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);
        }
    }

    // ─── STEP 1: Initiate PayOS payment ──────────────────────────────────────

    @Override
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userEmail) {
        User user = resolveUser(userEmail);

        // Resolve danh sách services (hỗ trợ multi-service)
        List<Integer> effectiveServiceIds = resolveServiceIds(request.getServiceId(), request.getServiceIds());

        validateBookingInputs(request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(), user.getId());

        // Tổng duration để check conflict
        int totalDuration = resolveTotalDuration(effectiveServiceIds);

        // Kiểm tra xem có service nào là boarding không
        boolean isBoarding = effectiveServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .anyMatch(s -> s != null && ("BOARDING".equalsIgnoreCase(s.getCategory()) || "Hotel".equalsIgnoreCase(s.getCategory())));

        // ── Kiểm tra trùng lịch pet ──────────────────────────────────────────
        checkPetConflict(request.getPetId(), request.getShopId(), request.getAppointmentDatetime(), totalDuration, isBoarding);

        // ── Kiểm tra trùng lịch staff (nếu có chọn staff) ────────────────────
        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(), totalDuration, isBoarding);
        } else {
            // Không chọn staff cụ thể → kiểm tra shop còn nhân viên rảnh không
            checkShopHasAvailableStaff(request.getShopId(), request.getAppointmentDatetime(), totalDuration, isBoarding);
        }

        long orderCode = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Booking" + orderCode % 100000;
        // Tổng giá tất cả services
        int rawAmount = resolveTotalPrice(effectiveServiceIds, request.getPetId(), request.getPetWeight()).intValue();

        // --- Voucher Logic ---
        if (request.getUserVoucherId() != null) {
            UserVoucher uv = userVoucherRepository.findById(request.getUserVoucherId())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
            
            if (uv.getUser().getId() != user.getId() || uv.isUsed()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            
            Voucher v = uv.getVoucher();
            if (v.getMinOrderValue() != null && rawAmount < v.getMinOrderValue()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            
            double discount = 0;
            if ("PERCENTAGE".equalsIgnoreCase(v.getDiscountType())) {
                discount = rawAmount * (v.getDiscountValue() / 100.0);
            } else {
                discount = v.getDiscountValue();
            }
            
            rawAmount -= discount;
        }
        // ---------------------

        int amountVnd = Math.max(rawAmount, 2000);
        if (amountVnd % 1000 != 0) amountVnd = ((amountVnd / 1000) + 1) * 1000;

        // ── Lưu vào Redis (TTL 30 phút) ──────────────────────────────────────
        PendingBooking pending = new PendingBooking(
                user.getId(), request.getShopId(), request.getServiceId(),
                effectiveServiceIds,
                request.getPetId(), request.getStaffId(),
                request.getAppointmentDatetime(), request.getCheckIn(), request.getCheckOut(),
                request.getNote(), amountVnd, description,
                request.getCheckOut(), request.getPetWeight(), request.getRoomType(),
                request.getUserVoucherId(), null
        );
        savePending(orderCode, pending);

        // ── Giữ slot staff trong Redis (atomic SET NX) ────────────────────────
        if (request.getStaffId() != null) {
            boolean locked = tryLockStaffSlot(request.getStaffId(),
                    request.getAppointmentDatetime(), orderCode);
            if (!locked) {
                // Slot vừa bị giữ bởi request khác trong khoảnh khắc này
                deletePending(orderCode);
                throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
            }
        }

        PayOSCreateResponse payosResponse;
        try {
            payosResponse = payOSService.createPaymentLink(
                    orderCode, amountVnd, description, returnUrl, cancelUrl);
        } catch (Exception e) {
            deletePending(orderCode);
            if (request.getStaffId() != null) {
                releaseStaffSlot(request.getStaffId(), request.getAppointmentDatetime());
            }
            log.error("PayOS createPaymentLink failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        if (payosResponse == null || !payosResponse.isSuccess() || payosResponse.getData() == null) {
            deletePending(orderCode);
            if (request.getStaffId() != null) {
                releaseStaffSlot(request.getStaffId(), request.getAppointmentDatetime());
            }
            log.error("PayOS error: code={}, desc={}",
                    payosResponse != null ? payosResponse.getCode() : "null",
                    payosResponse != null ? payosResponse.getDesc() : "null");
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        log.info("PayOS link created — orderCode={} checkoutUrl={}",
                orderCode, payosResponse.getData().getCheckoutUrl());

        return InitiatePaymentResponse.builder()
                .orderCode(orderCode)
                .checkoutUrl(payosResponse.getData().getCheckoutUrl())
                .qrCode(payosResponse.getData().getQrCode())
                .amount(amountVnd)
                .description(description)
                .build();
    }

    // ─── STEP 2: Confirm payment → create booking ─────────────────────────────

    @Override
    @Transactional
    public BookingResponse confirmPayment(long orderCode, String userEmail) {
        User user = resolveUser(userEmail);

        // Idempotent check
        bookingRepository.findByPayosOrderCode(orderCode).ifPresent(existing -> {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
        });

        // Query PayOS
        PayOSPaymentInfoResponse info;
        try {
            info = payOSService.getPaymentInfo(orderCode);
        } catch (Exception e) {
            log.error("PayOS getPaymentInfo failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        String payosStatus = info.getPaymentStatus();
        log.info("PayOS confirmPayment — orderCode={} status={}", orderCode, payosStatus);

        if (!"PAID".equals(payosStatus)) {
            deletePending(orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        // Lấy pending từ Redis
        PendingBooking pending = getPending(orderCode);
        if (pending == null) {
            log.warn("Pending booking not found in Redis for orderCode={}", orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (pending.getUserId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        // ── Kiểm tra lại trùng lịch (double-check trước khi lưu DB) ──────────
        List<Integer> pendingServiceIds = resolveServiceIds(pending.getServiceId(), pending.getServiceIds());
        int pendingTotalDuration = resolveTotalDuration(pendingServiceIds);
        Service service = serviceRepository.findById(pending.getServiceId()).get();

        boolean isBoarding = "BOARDING".equalsIgnoreCase(service.getCategory()) || "Hotel".equalsIgnoreCase(service.getCategory());
        checkPetConflict(pending.getPetId(), pending.getShopId(), pending.getAppointmentDatetime(), pendingTotalDuration, isBoarding);

        // Double-check staff conflict từ DB (slot Redis đã được giữ ở bước 1)
        if (pending.getStaffId() != null) {
            LocalDateTime ws = pending.getAppointmentDatetime();
            LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
            if (bookingRepository.countConflictingBookingForStaff(pending.getStaffId(), ws, we) > 0) {
                deletePending(orderCode);
                releaseStaffSlot(pending.getStaffId(), pending.getAppointmentDatetime());
                throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
            }
        }

        Shop shop   = shopRepository.findById(pending.getShopId()).get();
        Pet pet     = petRepository.findById(pending.getPetId()).get();
        Staff staff = pending.getStaffId() != null
                ? staffRepository.findById(pending.getStaffId()).orElse(null)
                : null;

        if (staff == null) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = pending.getAppointmentDatetime();
                LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
                
                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = bookingRepository.countConflictingBookingForStaff(s.getId(), ws, we) > 0;
                    String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                                 + pending.getAppointmentDatetime().withSecond(0).withNano(0).toString();
                    boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
                    return !dbBusy && !redisBusy;
                }).collect(Collectors.toList());

                if (!availableStaff.isEmpty()) {
                    staff = availableStaff.get(ThreadLocalRandom.current().nextInt(availableStaff.size()));
                } else {
                    staff = activeStaff.get(ThreadLocalRandom.current().nextInt(activeStaff.size()));
                }
            }
        }

        Double discountAmount = null;
        Voucher appliedVoucher = null;
        if (pending.getUserVoucherId() != null) {
            UserVoucher uv = userVoucherRepository.findById(pending.getUserVoucherId()).orElse(null);
            if (uv != null && !uv.isUsed()) {
                uv.setUsed(true);
                uv.setUsedAt(LocalDateTime.now());
                userVoucherRepository.save(uv);
                appliedVoucher = uv.getVoucher();
                
                int rawAmount = resolveTotalPrice(pendingServiceIds, pending.getPetId(), pending.getPetWeight()).intValue();
                if ("PERCENTAGE".equalsIgnoreCase(appliedVoucher.getDiscountType())) {
                    discountAmount = rawAmount * (appliedVoucher.getDiscountValue() / 100.0);
                } else {
                    discountAmount = appliedVoucher.getDiscountValue();
                }
            }
        }

        // ── Tạo hoặc Update Booking ───────────────────────────────────────────────────────
        java.util.Set<Service> servicesSet = pendingServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Booking booking;
        if (pending.getUpdateBookingId() != null) {
            // Đây là luồng thanh toán thêm cọc khi Update Booking
            booking = bookingRepository.findById(pending.getUpdateBookingId()).orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
            booking.setPet(pet);
            booking.setStaff(staff);
            booking.setServices(servicesSet);
            booking.setPetWeight(pending.getPetWeight());
            booking.setRoomType(pending.getRoomType());
            booking = bookingRepository.save(booking);
        } else {
            booking = Booking.builder()
                    .user(user).shop(shop).pet(pet).staff(staff)
                    .services(servicesSet)
                    .appointmentDatetime(pending.getAppointmentDatetime())
                    .checkOutDatetime(pending.getCheckOutDatetime())
                    .petWeight(pending.getPetWeight())
                    .roomType(pending.getRoomType())
                    .checkIn(pending.getCheckIn())
                    .checkOut(pending.getCheckOut())
                    .note(pending.getNote())
                    .status((staff != null && !"AUTO".equals(shop.getAssignmentMode())) ? "WAITING_SHOP_APPROVAL" : "CONFIRMED")
                    .payosOrderCode(orderCode)
                    .appliedVoucher(appliedVoucher)
                    .discountAmount(discountAmount)
                    .build();
            booking = bookingRepository.save(booking);
        }

        // ── Tạo Payment ───────────────────────────────────────────────────────
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(new BigDecimal(pending.getAmountVnd()))
                .method("PAYOS")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .description(pending.getDescription())
                .gatewayTransactionId(info.getData() != null ? info.getData().getId() : null)
                .build();
        paymentRepository.save(payment);

        // ── Ghi Transaction ───────────────────────────────────────────────────
        transactionRepository.save(Transaction.builder()
                .booking(booking)
                .shop(shop)
                .type("BOOKING_PAYMENT")
                .amount(new BigDecimal(pending.getAmountVnd()))
                .paymentMethod("PAYOS")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .gatewayTransactionId(info.getData() != null ? info.getData().getId() : null)
                .description("Thanh toán qua PayOS cho đơn #" + booking.getId())
                .completedAt(LocalDateTime.now())
                .build());

        deletePending(orderCode);

        // Giải phóng Redis slot (booking đã vào DB, không cần lock nữa)
        if (staff != null) {
            releaseStaffSlot(staff.getId(), pending.getAppointmentDatetime());
        }

        // --- Notification cho Shop Owner và Staff ---
        Notification notifOwner = Notification.builder()
                .user(shop.getOwner())
                .title("Có đơn đặt lịch mới")
                .content(String.format("Có đơn đặt lịch mới #%d từ khách hàng %s.", booking.getId(), user.getFullName()))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifOwner);
        messagingTemplate.convertAndSend("/topic/shop/" + shop.getId() + "/notifications", java.util.Map.of("message", "Có đơn hàng mới!"));

        try {
            if (shop.getEmail() != null && !shop.getEmail().isBlank()) {
                emailService.sendNewBookingToShopEmail(shop.getEmail(), booking);
            }
        } catch (Exception e) {
            log.warn("Failed to send new booking email to shop {}: {}", shop.getId(), e.getMessage());
        }

        try {
            java.util.List<User> admins = userRepository.findByRoleName("ADMIN");
            for (User admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    emailService.sendNewBookingToAdminEmail(admin.getEmail(), booking);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send new booking email to admin: {}", e.getMessage());
        }

        if (staff != null && staff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(staff.getUser())
                    .title("Bạn vừa được giao lịch hẹn mới")
                    .content(String.format("Bạn vừa được giao một lịch hẹn mới #%d.", booking.getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }
        // ------------------------------------------

        log.info("Booking {} CONFIRMED (PayOS) — orderCode={}", booking.getId(), orderCode);

        // ── Gửi hóa đơn email cho khách hàng ─────────────────────────────────
        try {
            emailService.sendBookingInvoiceEmail(
                    user.getEmail(),
                    booking,
                    new BigDecimal(pending.getAmountVnd()),
                    "PAYOS",
                    "Đã thanh toán thành công"
            );
        } catch (Exception e) {
            log.warn("Failed to send invoice email for booking {}: {}", booking.getId(), e.getMessage());
        }

        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse mockConfirmPayment(long orderCode, String userEmail) {
        User user = resolveUser(userEmail);

        // Idempotent check
        bookingRepository.findByPayosOrderCode(orderCode).ifPresent(existing -> {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
        });

        // Lấy pending từ Redis
        PendingBooking pending = getPending(orderCode);
        if (pending == null) {
            log.warn("Pending booking not found in Redis for orderCode={}", orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (pending.getUserId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        // ── Kiểm tra lại trùng lịch (double-check trước khi lưu DB) ──────────
        List<Integer> pendingServiceIds = resolveServiceIds(pending.getServiceId(), pending.getServiceIds());
        int pendingTotalDuration = resolveTotalDuration(pendingServiceIds);
        Service service = serviceRepository.findById(pending.getServiceId()).get();

        boolean isBoarding = "BOARDING".equalsIgnoreCase(service.getCategory()) || "Hotel".equalsIgnoreCase(service.getCategory());
        checkPetConflict(pending.getPetId(), pending.getShopId(), pending.getAppointmentDatetime(), pendingTotalDuration, isBoarding);

        // Double-check staff conflict từ DB (slot Redis đã được giữ ở bước 1)
        if (pending.getStaffId() != null) {
            LocalDateTime ws = pending.getAppointmentDatetime();
            LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
            if (bookingRepository.countConflictingBookingForStaff(pending.getStaffId(), ws, we) > 0) {
                deletePending(orderCode);
                releaseStaffSlot(pending.getStaffId(), pending.getAppointmentDatetime());
                throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
            }
        }

        Shop shop   = shopRepository.findById(pending.getShopId()).get();
        Pet pet     = petRepository.findById(pending.getPetId()).get();
        Staff staff = pending.getStaffId() != null
                ? staffRepository.findById(pending.getStaffId()).orElse(null)
                : null;

        if (staff == null) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = pending.getAppointmentDatetime();
                LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
                
                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = bookingRepository.countConflictingBookingForStaff(s.getId(), ws, we) > 0;
                    String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                                 + pending.getAppointmentDatetime().withSecond(0).withNano(0).toString();
                    boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
                    return !dbBusy && !redisBusy;
                }).collect(Collectors.toList());

                if (!availableStaff.isEmpty()) {
                    staff = availableStaff.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(availableStaff.size()));
                } else {
                    staff = activeStaff.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(activeStaff.size()));
                }
            }
        }

        Double discountAmount = null;
        Voucher appliedVoucher = null;
        if (pending.getUserVoucherId() != null) {
            UserVoucher uv = userVoucherRepository.findById(pending.getUserVoucherId()).orElse(null);
            if (uv != null && !uv.isUsed()) {
                uv.setUsed(true);
                uv.setUsedAt(LocalDateTime.now());
                userVoucherRepository.save(uv);
                appliedVoucher = uv.getVoucher();
                
                int rawAmount = resolveTotalPrice(pendingServiceIds, pending.getPetId(), pending.getPetWeight()).intValue();
                if ("PERCENTAGE".equalsIgnoreCase(appliedVoucher.getDiscountType())) {
                    discountAmount = rawAmount * (appliedVoucher.getDiscountValue() / 100.0);
                } else {
                    discountAmount = appliedVoucher.getDiscountValue();
                }
            }
        }

        // ── Tạo Booking ───────────────────────────────────────────────────────
        java.util.Set<Service> servicesSet = pendingServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Booking booking = Booking.builder()
                .user(user).shop(shop).pet(pet).staff(staff)
                .services(servicesSet)
                .appointmentDatetime(pending.getAppointmentDatetime())
                .checkIn(pending.getCheckIn())
                .checkOut(pending.getCheckOut())
                .note(pending.getNote())
                .petWeight(pending.getPetWeight())
                .roomType(pending.getRoomType())
                .status((staff != null && !"AUTO".equals(shop.getAssignmentMode())) ? "WAITING_SHOP_APPROVAL" : "CONFIRMED")
                .payosOrderCode(orderCode)
                .appliedVoucher(appliedVoucher)
                .discountAmount(discountAmount)
                .build();
        booking = bookingRepository.save(booking);

        // ── Tạo Payment ───────────────────────────────────────────────────────
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(new BigDecimal(pending.getAmountVnd()))
                .method("MOCK")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .description(pending.getDescription())
                .gatewayTransactionId("MOCK-" + orderCode)
                .build();
        paymentRepository.save(payment);

        // ── Ghi Transaction ───────────────────────────────────────────────────
        transactionRepository.save(Transaction.builder()
                .booking(booking)
                .shop(shop)
                .type("BOOKING_PAYMENT")
                .amount(new BigDecimal(pending.getAmountVnd()))
                .paymentMethod("MOCK")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .gatewayTransactionId("MOCK-" + orderCode)
                .description("Thanh toán giả lập cho đơn #" + booking.getId())
                .completedAt(LocalDateTime.now())
                .build());

        deletePending(orderCode);

        // Giải phóng Redis slot (booking đã vào DB, không cần lock nữa)
        if (staff != null) {
            releaseStaffSlot(staff.getId(), pending.getAppointmentDatetime());
        }

        // --- Notification cho Shop Owner và Staff ---
        Notification notifOwner = Notification.builder()
                .user(shop.getOwner())
                .title("Có đơn đặt lịch mới")
                .content(String.format("Có đơn đặt lịch mới #%d từ khách hàng %s.", booking.getId(), user.getFullName()))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifOwner);
        messagingTemplate.convertAndSend("/topic/shop/" + shop.getId() + "/notifications", java.util.Map.of("message", "Có đơn hàng mới!"));

        try {
            if (shop.getEmail() != null && !shop.getEmail().isBlank()) {
                emailService.sendNewBookingToShopEmail(shop.getEmail(), booking);
            }
        } catch (Exception e) {
            log.warn("Failed to send new booking email to shop {}: {}", shop.getId(), e.getMessage());
        }

        try {
            java.util.List<User> admins = userRepository.findByRoleName("ADMIN");
            for (User admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    emailService.sendNewBookingToAdminEmail(admin.getEmail(), booking);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send new booking email to admin: {}", e.getMessage());
        }

        if (staff != null && staff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(staff.getUser())
                    .title("Bạn vừa được giao lịch hẹn mới")
                    .content(String.format("Bạn vừa được giao một lịch hẹn mới #%d.", booking.getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }

        log.info("Booking {} CONFIRMED (MOCK) — orderCode={}", booking.getId(), orderCode);

        // ── Gửi hóa đơn email cho khách hàng ─────────────────────────────────
        try {
            emailService.sendBookingInvoiceEmail(
                    user.getEmail(),
                    booking,
                    new BigDecimal(pending.getAmountVnd()),
                    "MOCK",
                    "Đã thanh toán thành công"
            );
        } catch (Exception e) {
            log.warn("Failed to send invoice email for booking {}: {}", booking.getId(), e.getMessage());
        }

        return toResponse(booking);
    }

    // ─── CASH booking (2-step: 10% deposit via PayOS + 90% cash at venue) ────

    /**
     * CASH STEP 1: Validate inputs, create PayOS link for 10% deposit (= admin commission).
     * Pending booking is stored in Redis (same as PayOS flow).
     * NO booking saved to DB yet.
     */
    @Override
    public InitiatePaymentResponse initiateCashDeposit(BookingCreationRequest request, String userEmail) {
        User user = resolveUser(userEmail);

        // Resolve danh sách services (hỗ trợ multi-service)
        List<Integer> effectiveServiceIds = resolveServiceIds(request.getServiceId(), request.getServiceIds());

        validateBookingInputs(request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(), user.getId());

        // Tổng duration để check conflict
        int totalDuration = resolveTotalDuration(effectiveServiceIds);

        // Kiểm tra xem có service nào là boarding không
        boolean isBoarding = effectiveServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .anyMatch(s -> s != null && ("BOARDING".equalsIgnoreCase(s.getCategory()) || "Hotel".equalsIgnoreCase(s.getCategory())));

        // ── Kiểm tra trùng lịch pet ──────────────────────────────────────────
        checkPetConflict(request.getPetId(), request.getShopId(), request.getAppointmentDatetime(), totalDuration, isBoarding);

        // ── Kiểm tra trùng lịch staff ─────────────────────────────────────────
        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(), totalDuration, isBoarding);
        } else {
            // Không chọn staff cụ thể → kiểm tra shop còn nhân viên rảnh không
            checkShopHasAvailableStaff(request.getShopId(), request.getAppointmentDatetime(), totalDuration, isBoarding);
        }

        // ── Tính tiền cọc = tổng phí hoa hồng admin cho các dịch vụ ─────────
        BigDecimal rawDeposit = walletService.calculateAdminCommission(effectiveServiceIds, request.getPetId(), request.getPetWeight());
        int depositVnd = rawDeposit.setScale(0, java.math.RoundingMode.CEILING).intValue();
        depositVnd = Math.max(depositVnd, 2000);
        if (depositVnd % 1000 != 0) depositVnd = ((depositVnd / 1000) + 1) * 1000;

        long orderCode  = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Deposit" + orderCode % 100000;

        // ── Lưu vào Redis (TTL 30 phút) — tái dùng PendingBooking ────────────
        PendingBooking pending = new PendingBooking(
                user.getId(), request.getShopId(), request.getServiceId(),
                effectiveServiceIds,
                request.getPetId(), request.getStaffId(),
                request.getAppointmentDatetime(), request.getCheckIn(), request.getCheckOut(),
                request.getNote(), depositVnd, description,
                request.getCheckOut(), request.getPetWeight(), request.getRoomType(), null, null
        );
        redisTemplate.opsForValue().set(
                CASH_PENDING_PREFIX + orderCode, pending,
                PENDING_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);

        // ── Giữ slot staff trong Redis (atomic SET NX) ────────────────────────
        if (request.getStaffId() != null) {
            boolean locked = tryLockStaffSlot(request.getStaffId(),
                    request.getAppointmentDatetime(), orderCode);
            if (!locked) {
                redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
                throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
            }
        }



        PayOSCreateResponse payosResponse;
        try {
            payosResponse = payOSService.createPaymentLink(
                    orderCode, depositVnd, description, returnUrl, cancelUrl);
        } catch (Exception e) {
            redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
            if (request.getStaffId() != null) {
                releaseStaffSlot(request.getStaffId(), request.getAppointmentDatetime());
            }
            log.error("PayOS createPaymentLink failed for cash deposit: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        if (payosResponse == null || !payosResponse.isSuccess() || payosResponse.getData() == null) {
            redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
            if (request.getStaffId() != null) {
                releaseStaffSlot(request.getStaffId(), request.getAppointmentDatetime());
            }
            log.error("PayOS error for cash deposit: code={}, desc={}",
                    payosResponse != null ? payosResponse.getCode() : "null",
                    payosResponse != null ? payosResponse.getDesc() : "null");
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        log.info("PayOS cash deposit link created — orderCode={} checkoutUrl={}",
                orderCode, payosResponse.getData().getCheckoutUrl());

        return InitiatePaymentResponse.builder()
                .orderCode(orderCode)
                .checkoutUrl(payosResponse.getData().getCheckoutUrl())
                .qrCode(payosResponse.getData().getQrCode())
                .amount(depositVnd)
                .description(description)
                .build();
    }

    /**
     * CASH STEP 2: Verify 10% deposit paid via PayOS → create booking.
     * Booking status = CONFIRMED. Remaining 90% collected in cash at venue.
     * On COMPLETED: wallet credits shop with 90% of full service price.
     */
    @Override
    @Transactional
    public BookingResponse confirmCashDeposit(long orderCode, String userEmail) {
        User user = resolveUser(userEmail);

        // Idempotent check
        bookingRepository.findByPayosOrderCode(orderCode).ifPresent(existing -> {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
        });

        // Lấy pending từ Redis
        Object raw = redisTemplate.opsForValue().get(CASH_PENDING_PREFIX + orderCode);
        if (raw == null) {
            log.warn("Cash pending booking not found in Redis for orderCode={}", orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }
        PendingBooking pending = (raw instanceof PendingBooking pb) ? pb : null;
        if (pending == null) throw new AppException(ErrorCode.BOOKING_NOT_FOUND);

        if (pending.getUserId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        // Query PayOS
        PayOSPaymentInfoResponse info;
        try {
            info = payOSService.getPaymentInfo(orderCode);
        } catch (Exception e) {
            log.error("PayOS getPaymentInfo failed for cash deposit: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        String payosStatus = info.getPaymentStatus();
        String gatewayTxId = info.getData() != null ? info.getData().getId() : null;

        log.info("PayOS confirmCashDeposit — orderCode={} status={}", orderCode, payosStatus);

        if (!"PAID".equals(payosStatus)) {
            redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
            if (pending.getStaffId() != null) {
                releaseStaffSlot(pending.getStaffId(), pending.getAppointmentDatetime());
            }
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        // ── Kiểm tra lại trùng lịch ───────────────────────────────────────────
        List<Integer> pendingServiceIds = resolveServiceIds(pending.getServiceId(), pending.getServiceIds());
        int pendingTotalDuration = resolveTotalDuration(pendingServiceIds);
        Service service = serviceRepository.findById(pending.getServiceId()).get();
        boolean isBoarding = "BOARDING".equalsIgnoreCase(service.getCategory()) || "Hotel".equalsIgnoreCase(service.getCategory());
        checkPetConflict(pending.getPetId(), pending.getShopId(), pending.getAppointmentDatetime(), pendingTotalDuration, isBoarding);

        if (pending.getStaffId() != null) {
            LocalDateTime ws = pending.getAppointmentDatetime();
            LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
            if (!isBoarding && bookingRepository.countConflictingBookingForStaff(pending.getStaffId(), ws, we) > 0) {
                redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
                releaseStaffSlot(pending.getStaffId(), pending.getAppointmentDatetime());
                throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
            }
        }

        Shop  shop = shopRepository.findById(pending.getShopId()).get();
        Pet   pet  = petRepository.findById(pending.getPetId()).get();
        Staff staff = pending.getStaffId() != null
                ? staffRepository.findById(pending.getStaffId()).orElse(null)
                : null;

        if (staff == null) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = pending.getAppointmentDatetime();
                LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = !isBoarding && bookingRepository.countConflictingBookingForStaff(s.getId(), ws, we) > 0;
                    String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                            + pending.getAppointmentDatetime().withSecond(0).withNano(0).toString();
                    boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
                    return !dbBusy && !redisBusy;
                }).collect(Collectors.toList());
                if (!availableStaff.isEmpty()) {
                    staff = availableStaff.get(ThreadLocalRandom.current().nextInt(availableStaff.size()));
                } else {
                    staff = activeStaff.get(ThreadLocalRandom.current().nextInt(activeStaff.size()));
                }
            }
        }

        // ── Tạo Booking ───────────────────────────────────────────────────────
        java.util.Set<Service> servicesSet = pendingServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Booking booking = Booking.builder()
                .user(user).shop(shop).pet(pet).staff(staff)
                .services(servicesSet)
                .appointmentDatetime(pending.getAppointmentDatetime())
                .checkOutDatetime(pending.getCheckOutDatetime())
                .petWeight(pending.getPetWeight())
                .roomType(pending.getRoomType())
                .checkIn(pending.getCheckIn())
                .checkOut(pending.getCheckOut())
                .note(pending.getNote())
                .petWeight(pending.getPetWeight())
                .roomType(pending.getRoomType())
                .status((staff != null && !"AUTO".equals(shop.getAssignmentMode())) ? "WAITING_SHOP_APPROVAL" : "CONFIRMED")
                .payosOrderCode(orderCode)
                .build();
        booking = bookingRepository.save(booking);

        // ── Tạo Payment: ghi nhận tiền cọc đã thanh toán qua PayOS ───────
        BigDecimal depositAmount = new BigDecimal(pending.getAmountVnd());
        String fullPriceStr = servicesSet.stream().map(s -> s.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString();
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(depositAmount)
                .method("CASH_DEPOSIT")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .description(String.format(
                        "Tiền cọc (PayOS) cho đơn thu tiền mặt #%d — tổng giá: %s VND",
                        booking.getId(), fullPriceStr))
                .gatewayTransactionId(gatewayTxId)
                .build();
        paymentRepository.save(payment);

        // ── Ghi Transaction: tiền cọc ─────────────────────────────────────
        transactionRepository.save(Transaction.builder()
                .booking(booking)
                .shop(shop)
                .type("BOOKING_PAYMENT")
                .amount(depositAmount)
                .paymentMethod("CASH_DEPOSIT")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .gatewayTransactionId(gatewayTxId)
                .description(String.format(
                        "Đã thanh toán cọc (PayOS) cho đơn #%d", booking.getId()))
                .completedAt(LocalDateTime.now())
                .build());

        redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
        if (staff != null) {
            releaseStaffSlot(staff.getId(), pending.getAppointmentDatetime());
        }

        // --- Notification cho Shop Owner và Staff ---
        Notification notifOwner = Notification.builder()
                .user(shop.getOwner())
                .title("Có đơn đặt lịch mới (Cọc tiền mặt)")
                .content(String.format("Có đơn đặt lịch mới #%d từ khách hàng %s.", booking.getId(), user.getFullName()))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifOwner);
        messagingTemplate.convertAndSend("/topic/shop/" + shop.getId() + "/notifications", java.util.Map.of("message", "Có đơn hàng mới!"));

        try {
            if (shop.getEmail() != null && !shop.getEmail().isBlank()) {
                emailService.sendNewBookingToShopEmail(shop.getEmail(), booking);
            }
        } catch (Exception e) {
            log.warn("Failed to send new booking email to shop {}: {}", shop.getId(), e.getMessage());
        }

        try {
            java.util.List<User> admins = userRepository.findByRoleName("ADMIN");
            for (User admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                    emailService.sendNewBookingToAdminEmail(admin.getEmail(), booking);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send new booking email to admin: {}", e.getMessage());
        }

        if (staff != null && staff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(staff.getUser())
                    .title("Bạn vừa được giao lịch hẹn mới")
                    .content(String.format("Bạn vừa được giao một lịch hẹn mới #%d.", booking.getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }
        // ------------------------------------------

        log.info("Cash booking {} CONFIRMED — deposit={}VND, remaining {}VND to be collected in cash",
                booking.getId(), depositAmount, new BigDecimal(fullPriceStr).subtract(depositAmount));

        // ── Gửi email xác nhận đặt cọc cho khách hàng ────────────────────────
        try {
            emailService.sendBookingInvoiceEmail(
                    user.getEmail(),
                    booking,
                    depositAmount,
                    "CASH_DEPOSIT",
                    "Đã thanh toán cọc thành công — phần còn lại thanh toán tại quầy"
            );
        } catch (Exception e) {
            log.warn("Failed to send deposit invoice email for booking {}: {}", booking.getId(), e.getMessage());
        }

        return toResponse(booking);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Override
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = resolveUser(userEmail);
        return bookingRepository.findByUserIdWithServices(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public com.sang.sourcepattern.dto.response.PageResponse<BookingResponse> getMyBookings(String userEmail, int page, int size, String status) {
        User user = resolveUser(userEmail);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Booking> pageData;

        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            pageData = bookingRepository.findByUserIdWithServices(user.getId(), pageable);
        } else {
            List<String> statuses;
            if ("upcoming".equalsIgnoreCase(status)) {
                statuses = List.of("CONFIRMED", "WAITING_SHOP_APPROVAL", "PENDING_PAYMENT");
                pageData = bookingRepository.findByUserIdAndStatusInWithServices(user.getId(), statuses, pageable);
            } else if ("active".equalsIgnoreCase(status)) {
                statuses = List.of("IN_PROGRESS");
                pageData = bookingRepository.findByUserIdAndStatusInWithServices(user.getId(), statuses, pageable);
            } else if ("completed".equalsIgnoreCase(status)) {
                statuses = List.of("COMPLETED");
                pageData = bookingRepository.findByUserIdAndStatusInWithServices(user.getId(), statuses, pageable);
            } else if ("cancelled".equalsIgnoreCase(status)) {
                statuses = List.of("CANCELLED", "WAITING_REFUND", "CANCEL_REQUESTED");
                pageData = bookingRepository.findByUserIdAndStatusInWithServices(user.getId(), statuses, pageable);
            } else {
                pageData = bookingRepository.findByUserIdWithServices(user.getId(), pageable);
            }
        }

        return com.sang.sourcepattern.dto.response.PageResponse.<BookingResponse>builder()
                .content(pageData.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
                .page(pageData.getNumber() + 1)
                .size(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .last(pageData.isLast())
                .build();
    }

    @Override
    public BookingResponse getBookingById(int bookingId, String userEmail) {
        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findByIdWithServices(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
                
        boolean isCustomer = booking.getUser().getId() == user.getId();
        boolean isAssignedStaff = booking.getStaff() != null && booking.getStaff().getUser() != null && booking.getStaff().getUser().getId() == user.getId();
        boolean isShopOwner = booking.getShop().getOwner() != null && booking.getShop().getOwner().getId() == user.getId();
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        
        if (!isCustomer && !isAssignedStaff && !isShopOwner && !isAdmin) {
            throw new AppException(ErrorCode.UNAUTHENTICATED); // or custom error code
        }
        
        return toResponse(booking);
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Object updateBooking(int bookingId, com.sang.sourcepattern.dto.request.UpdateBookingRequest request, String userEmail) {
        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        if (!"CONFIRMED".equals(booking.getStatus()) && !"WAITING_SHOP_APPROVAL".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Check if pet changed
        if (request.getPetId() != null && (booking.getPet() == null || booking.getPet().getId() != request.getPetId())) {
            Pet pet = petRepository.findById(request.getPetId())
                    .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));
            if (pet.getOwner().getId() != user.getId()) {
                throw new AppException(ErrorCode.PET_NOT_BELONG_TO_USER);
            }
            booking.setPet(pet);
        }

        // Check if staff changed
        if (request.getStaffId() != null && (booking.getStaff() == null || booking.getStaff().getId() != request.getStaffId())) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            if (staff.getShop().getId() != booking.getShop().getId()) {
                throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);
            }
            booking.setStaff(staff);
        } else if (request.getStaffId() == null && booking.getStaff() != null) {
            booking.setStaff(null);
        }

        // Check if services changed
        boolean servicesChanged = false;
        List<Integer> currentServiceIds = booking.getServices().stream().map(Service::getId).collect(Collectors.toList());
        List<Integer> newServiceIds = request.getServiceIds();

        if (newServiceIds != null && !newServiceIds.isEmpty()) {
            if (currentServiceIds.size() != newServiceIds.size() || !currentServiceIds.containsAll(newServiceIds)) {
                servicesChanged = true;
            }
        }

        boolean boardingDetailsChanged = false;
        if (request.getPetWeight() != null && !request.getPetWeight().equals(booking.getPetWeight())) {
            boardingDetailsChanged = true;
        }
        if (request.getRoomType() != null && !request.getRoomType().equals(booking.getRoomType())) {
            boardingDetailsChanged = true;
        }

        if (!servicesChanged && !boardingDetailsChanged) {
            boolean wantsToPayBalance = "PAYOS".equals(request.getPaymentMethod());
            if (!wantsToPayBalance) {
                // Only pet/staff changed -> Save and return BookingResponse
                bookingRepository.save(booking);
                return toResponse(booking);
            }
        }

        // Services changed
        List<Service> newServices = serviceRepository.findAllById(newServiceIds);
        if (newServices.size() != newServiceIds.size()) {
            throw new AppException(ErrorCode.SERVICE_NOT_FOUND);
        }

        java.math.BigDecimal newRawAmount = java.math.BigDecimal.ZERO;
        for (Service s : newServices) {
            java.math.BigDecimal price = resolveSingleServicePrice(s, request.getPetId(), request.getPetWeight());
            if (price != null) newRawAmount = newRawAmount.add(price);
        }

        int newAmountVnd = newRawAmount.intValue();
        int paidAmount = 0;
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
        if (payment != null && "SUCCESS".equals(payment.getStatus())) {
            paidAmount = payment.getAmount().intValue();
        }

        if (newAmountVnd <= paidAmount) {
            // No extra payment needed
            booking.setServices(new java.util.HashSet<>(newServices));
            if (request.getPetWeight() != null) booking.setPetWeight(request.getPetWeight());
            if (request.getRoomType() != null) booking.setRoomType(request.getRoomType());
            bookingRepository.save(booking);
            return toResponse(booking);
        }

        // Extra payment needed
        int extraAmount = newAmountVnd - paidAmount;
        long orderCode = java.util.concurrent.ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Update " + booking.getId();
        if (description.length() > 25) description = description.substring(0, 25);

        PendingBooking pending = new PendingBooking(
                user.getId(), booking.getShop().getId(), newServiceIds.get(0),
                newServiceIds, request.getPetId() != null ? request.getPetId() : booking.getPet().getId(), 
                request.getStaffId() != null ? request.getStaffId() : (booking.getStaff() != null ? booking.getStaff().getId() : null),
                booking.getAppointmentDatetime(), booking.getCheckIn(), booking.getCheckOut(),
                request.getNote() != null ? request.getNote() : booking.getNote(), 
                extraAmount, description,
                booking.getCheckOutDatetime(), 
                request.getPetWeight() != null ? request.getPetWeight() : booking.getPetWeight(), 
                request.getRoomType() != null ? request.getRoomType() : booking.getRoomType(),
                null, booking.getId()
        );
        savePending(orderCode, pending);

        com.sang.sourcepattern.dto.response.PayOSCreateResponse payosResponse;
        try {
            payosResponse = payOSService.createPaymentLink(
                    orderCode, extraAmount, description, returnUrl, cancelUrl);
        } catch (Exception e) {
            deletePending(orderCode);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        if (payosResponse == null) {
            deletePending(orderCode);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        return InitiatePaymentResponse.builder()
                .orderCode(orderCode)
                .checkoutUrl(payosResponse.getData().getCheckoutUrl())
                .qrCode(payosResponse.getData().getQrCode())
                .amount(extraAmount)
                .description(description)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(int bookingId, String userEmail) {
        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findByIdWithServices(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        boolean isUser  = user.getRoles().stream().anyMatch(r -> "USER".equals(r.getName()));
        boolean isOwner = user.getRoles().stream().anyMatch(r -> "SHOP_OWNER".equals(r.getName()));

        if (isUser && booking.getUser().getId() != user.getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);

        if (isOwner) {
            Shop shop = shopRepository.findByOwnerEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
            if (booking.getShop().getId() != shop.getId())
                throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        }

        if (!isUser && !isOwner)
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Không cho phép hủy booking ở trạng thái COMPLETED hoặc IN_PROGRESS
        if ("COMPLETED".equals(booking.getStatus()) || "IN_PROGRESS".equals(booking.getStatus()))
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);

        boolean wasCancelRequested = "CANCEL_REQUESTED".equals(booking.getStatus());
        
        if (wasCancelRequested) {
            booking.setStatus("WAITING_REFUND");
        } else {
            booking.setStatus("CANCELLED");
        }
        bookingRepository.save(booking);
        sendBookingUpdateEvent(booking.getShop().getId(), bookingId);

        if (wasCancelRequested) {
            List<User> admins = userRepository.findByRoleName("ADMIN");
            String broadcastId = UUID.randomUUID().toString();
            String bankInfo = (booking.getBankName() != null && !booking.getBankName().isBlank())
                    ? String.format("%s | %s | %s", booking.getBankName(), booking.getAccountHolder(), booking.getBankAccount())
                    : "Chưa có thông tin ngân hàng";
                    
            BigDecimal totalPrice = BigDecimal.ZERO;
            if (booking.getServices() != null && !booking.getServices().isEmpty()) {
                for (com.sang.sourcepattern.entity.Service s : booking.getServices()) {
                    totalPrice = totalPrice.add(resolveSingleServicePrice(s, booking.getPet() != null ? booking.getPet().getId() : null, booking.getPetWeight()));
                }
            }
                    
            List<Integer> serviceIds = booking.getServices().stream()
                    .map(com.sang.sourcepattern.entity.Service::getId)
                    .collect(Collectors.toList());
            BigDecimal deposit = walletService.calculateAdminCommission(serviceIds, booking.getPet() != null ? booking.getPet().getId() : null, booking.getPetWeight());
            
            long hoursToAppointment = 0;
            if (booking.getAppointmentDatetime() != null) {
                LocalDateTime requestTime = booking.getCancellationRequestedAt() != null ? booking.getCancellationRequestedAt() : LocalDateTime.now();
                hoursToAppointment = java.time.Duration.between(requestTime, booking.getAppointmentDatetime()).toHours();
            }
            
            BigDecimal refundAmount;
            if (hoursToAppointment >= 5) {
                // Hủy trước 5 tiếng: Được hoàn lại tiền dịch vụ, trừ tiền cọc
                refundAmount = totalPrice.subtract(deposit);
            } else {
                // Hủy sau 5 tiếng: Mất cọc + mất 50% tiền thiệt hại cho shop
                BigDecimal penalty = totalPrice.multiply(new BigDecimal("0.5"));
                refundAmount = totalPrice.subtract(deposit).subtract(penalty);
                
                // Cộng tiền bồi thường này vào ví Shop
                walletService.creditShopPenalty(bookingId, penalty, "hủy muộn");
            }
            
            if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
                refundAmount = BigDecimal.ZERO;
            }

            booking.setRefundAmount(refundAmount);
            bookingRepository.save(booking);

            String amountText = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(refundAmount);
            String content = String.format("Đơn hủy lịch #%d đã được chấp nhận. Vui lòng hoàn tiền cho khách hàng %s về: %s. Số tiền: %s.",
                    bookingId,
                    booking.getUser().getFullName(),
                    bankInfo,
                    amountText);
            List<Notification> notifications = admins.stream()
                    .map(admin -> Notification.builder()
                            .user(admin)
                            .title("Yêu cầu hoàn tiền khách hàng")
                            .content(content)
                            .broadcastId(broadcastId)
                            .notificationType(Notification.NotificationType.SYSTEM)
                            .build())
                    .collect(Collectors.toList());
            notificationRepository.saveAll(notifications);
            
            // Thông báo cho user
            Notification notifToUser = Notification.builder()
                    .user(booking.getUser())
                    .title("Đơn hủy đang đợi hoàn tiền")
                    .content("Shop đã duyệt đơn hủy lịch #" + bookingId + " của bạn. Hệ thống có thể sẽ xử lý hoàn tiền trong vòng vài ngày.")
                    .notificationType(Notification.NotificationType.SYSTEM)
                    .build();
            notificationRepository.save(notifToUser);
        }

        // Cập nhật Payment
        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            p.setStatus("CANCELLED");
            paymentRepository.save(p);
        });

        // Cập nhật Transaction
        transactionRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream().findFirst().ifPresent(t -> {
                    t.setStatus("CANCELLED");
                    t.setNote("Cancelled by " + userEmail);
                    transactionRepository.save(t);
                });

        cameraService.stopStream(bookingId);
        log.info("Booking {} CANCELLED by {}", bookingId, userEmail);
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse updateBankInfo(int bookingId, String bankName, String bankAccount, String accountHolder, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (user.getId() != booking.getUser().getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        if (!"WAITING_REFUND".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Only allow when WAITING_REFUND
        }

        booking.setBankName(bankName);
        booking.setBankAccount(bankAccount);
        booking.setAccountHolder(accountHolder);

        booking = bookingRepository.save(booking);
        return toResponse(booking);
    }
    public BookingResponse requestBookingCancellation(int bookingId, String reason, String bankName, String bankAccount, String accountHolder, String userEmail) {
        if (reason == null || reason.isBlank() || bankName == null || bankName.isBlank() || bankAccount == null || bankAccount.isBlank() || accountHolder == null || accountHolder.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findByIdWithServices(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getUser().getId() != user.getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && "CASH_DEPOSIT".equals(payment.getMethod())) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Deposit bookings cannot request refund
        }

        if ("COMPLETED".equals(booking.getStatus()) || "IN_PROGRESS".equals(booking.getStatus()))
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
        if ("CANCELLED".equals(booking.getStatus()))
            throw new AppException(ErrorCode.BOOKING_CANCELLED);
        if ("CANCEL_REQUESTED".equals(booking.getStatus()))
            throw new AppException(ErrorCode.REQUEST_ALREADY_PROCESSED);

        booking.setStatus("CANCEL_REQUESTED");
        booking.setCancellationReason(reason);
        booking.setCancellationRequestedAt(LocalDateTime.now());
        booking.setBankName(bankName);
        booking.setBankAccount(bankAccount);
        booking.setAccountHolder(accountHolder);
        bookingRepository.save(booking);
        sendBookingUpdateEvent(booking.getShop().getId(), bookingId);

        log.info("Booking {} CANCEL REQUESTED by {}", bookingId, userEmail);
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse shopCancelBooking(int bookingId, String reason, String requesterEmail) {
        if (reason == null || reason.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Shop shop = shopRepository.findByOwnerEmail(requesterEmail)
                .orElse(null);

        if (shop == null) {
            // Check if requester is staff
            Staff staff = staffRepository.findByUserEmail(requesterEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
            shop = staff.getShop();
        }

        Booking booking = bookingRepository.findByIdWithServices(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getShop().getId() != shop.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        }

        if ("COMPLETED".equals(booking.getStatus()) || "CANCELLED".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Calculate penalty and refund
        // - refundAmount: số tiền Admin cần hoàn cho Khách (= toàn bộ tiền khách đã trả)
        // - penalty:      số tiền phạt trừ vào VÍ của Shop (= toàn bộ tiền khách đã trả, vì Shop có lỗi)
        BigDecimal penalty = BigDecimal.ZERO;
        BigDecimal refundAmount = BigDecimal.ZERO;
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment != null && "SUCCESS".equals(payment.getStatus())) {
            refundAmount = payment.getAmount();
            penalty = payment.getAmount(); // Shop chịu 100% — bằng đúng số tiền khách đã trả
        }

        // Shop cancels booking:
        // Set to WAITING_REFUND so Admin can refund user.
        booking.setStatus("WAITING_REFUND");
        booking.setCancellationReason(reason);
        booking.setRefundAmount(refundAmount);
        bookingRepository.save(booking);
        sendBookingUpdateEvent(booking.getShop().getId(), bookingId);

        cameraService.stopStream(bookingId);

        // Deduct penalty from shop
        if (penalty.compareTo(BigDecimal.ZERO) > 0) {
            walletService.deductShopPenalty(shop.getId(), penalty, bookingId);
        }

        // Notify Admin to refund User
        List<User> admins = userRepository.findByRoleName("ADMIN");
        String broadcastId = UUID.randomUUID().toString();
        String amountText = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(refundAmount);
        String content = String.format("Shop %s đã chủ động hủy đơn #%d. Vui lòng hoàn lại toàn bộ số tiền %s cho khách hàng %s. Shop đã bị trừ phí phạt mất cọc.",
                shop.getShopName(),
                bookingId,
                amountText,
                booking.getUser() != null ? booking.getUser().getFullName() : "khách lẻ");

        List<Notification> notifications = admins.stream()
                .map(admin -> Notification.builder()
                        .user(admin)
                        .title("Shop tự hủy đơn - Yêu cầu hoàn tiền")
                        .content(content)
                        .broadcastId(broadcastId)
                        .notificationType(Notification.NotificationType.SYSTEM)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);

        // Notify User
        if (booking.getUser() != null) {
            Notification notifToUser = Notification.builder()
                    .user(booking.getUser())
                    .title("Shop đã hủy lịch của bạn")
                    .content(String.format("Shop %s đã hủy lịch #%d của bạn với lý do: %s. Hệ thống sẽ hoàn lại 100%% tiền cọc cho bạn sớm nhất.",
                            shop.getShopName(), bookingId, reason))
                    .notificationType(Notification.NotificationType.SYSTEM)
                    .build();
            notificationRepository.save(notifToUser);
        }

        log.info("Booking {} SHOP CANCELLED by {} - Reason: {}", bookingId, requesterEmail, reason);
        return toResponse(booking);
    }

    // ─── Staff list ───────────────────────────────────────────────────────────

    @Override
    public List<StaffResponse> getShopStaff(int shopId) {
        shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return staffRepository.findByShopIdAndIsActiveTrue(shopId).stream()
                .map(s -> StaffResponse.builder()
                        .id(s.getId()).shopId(s.getShop().getId())
                        .fullName(s.getFullName()).role(s.getRole())
                        .phone(s.getPhone()).specialization(s.getSpecialization())
                        .isActive(s.isActive()).build())
                .collect(Collectors.toList());
    }

    @Override
    public List<StaffResponse> getShopStaffWithAvailability(int shopId,
                                                             LocalDateTime appointmentDatetime,
                                                             int durationMinutes) {
        shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        return staffRepository.findByShopIdAndIsActiveTrue(shopId).stream()
                .map(s -> {
                    boolean available = true;
                    if (appointmentDatetime != null) {
                        LocalDateTime ws = appointmentDatetime;
                        LocalDateTime we = appointmentDatetime.plusMinutes(durationMinutes);

                        // Check DB
                        boolean dbBusy = bookingRepository.countConflictingBookingForStaff(s.getId(), ws, we) > 0;

                        // Check Redis slot
                        String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                                + appointmentDatetime.withSecond(0).withNano(0).toString();
                        boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));

                        available = !dbBusy && !redisBusy;
                    }
                    return StaffResponse.builder()
                            .id(s.getId()).shopId(s.getShop().getId())
                            .fullName(s.getFullName()).role(s.getRole())
                            .phone(s.getPhone()).specialization(s.getSpecialization())
                            .isActive(s.isActive())
                            .available(available)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkPetAvailability(int petId, LocalDateTime appointmentDatetime, int durationMinutes) {
        LocalDateTime windowStart = appointmentDatetime;
        LocalDateTime windowEnd   = appointmentDatetime.plusMinutes(durationMinutes);
        return bookingRepository.countStrictConflictingBookingForPet(petId, windowStart, windowEnd) == 0;
    }

    @Override
    public List<BookingResponse> getShopBookings(String ownerEmail, LocalDateTime start, LocalDateTime end) {
        int shopId;
        
        var shopOpt = shopRepository.findByOwnerEmail(ownerEmail);
        if (shopOpt.isPresent()) {
            shopId = shopOpt.get().getId();
        } else {
            var staffOpt = staffRepository.findByUserEmail(ownerEmail);
            if (staffOpt.isPresent()) {
                shopId = staffOpt.get().getShop().getId();
            } else {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
        }

        List<Booking> bookings = (start != null && end != null)
                ? bookingRepository.findByShopIdAndDatetimeBetweenWithServices(shopId, start, end)
                : bookingRepository.findByShopIdWithServices(shopId);

        return bookings.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public com.sang.sourcepattern.dto.response.PageResponse<BookingResponse> getShopBookingsPaged(String ownerEmail, LocalDateTime start, LocalDateTime end, int page) {
        int shopId;
        var shopOpt = shopRepository.findByOwnerEmail(ownerEmail);
        if (shopOpt.isPresent()) {
            shopId = shopOpt.get().getId();
        } else {
            var staffOpt = staffRepository.findByUserEmail(ownerEmail);
            if (staffOpt.isPresent()) {
                shopId = staffOpt.get().getShop().getId();
            } else {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
        }
        List<Booking> all = (start != null && end != null)
                ? bookingRepository.findByShopIdAndDatetimeBetweenWithServices(shopId, start, end)
                : bookingRepository.findByShopIdWithServices(shopId);
        // Sort by appointmentDatetime desc
        all.sort((a, b) -> {
            if (b.getAppointmentDatetime() == null) return -1;
            if (a.getAppointmentDatetime() == null) return 1;
            return b.getAppointmentDatetime().compareTo(a.getAppointmentDatetime());
        });
        int pageSize = 10;
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int fromIndex = Math.min(page * pageSize, total);
        int toIndex   = Math.min(fromIndex + pageSize, total);
        List<BookingResponse> content = all.subList(fromIndex, toIndex).stream()
                .map(this::toResponse).collect(Collectors.toList());
        return com.sang.sourcepattern.dto.response.PageResponse.<BookingResponse>builder()
                .content(content)
                .page(page)
                .size(pageSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(page >= totalPages - 1)
                .build();
    }

    // ─── Available time slots ─────────────────────────────────────────────────

    /**
     * Trả về danh sách các time slot còn available trong ngày cho shop.
     *
     * Logic:
     * 1. Lấy openTime / closeTime của shop (format "HH:mm").
     *    Nếu shop không có openTime/closeTime → dùng mặc định 08:00 – 20:00.
     * 2. Sinh ra tất cả các slot từ openTime đến closeTime - durationMinutes,
     *    bước nhảy = durationMinutes.
     * 3. Với mỗi slot, kiểm tra xem có ít nhất 1 staff active còn rảnh không
     *    (check cả DB và Redis).
     * 4. Chỉ trả về các slot có ít nhất 1 staff rảnh.
     */
    @Override
    public List<LocalDateTime> getAvailableTimeSlots(int shopId, java.time.LocalDate date, int durationMinutes) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // ── Kiểm tra ngày nghỉ (Off Days) ─────────────────────────────────────
        if (shop.getOffDays() != null && !shop.getOffDays().trim().isEmpty()) {
            String targetDateStr = date.toString();
            if (java.util.Arrays.asList(shop.getOffDays().split(",")).contains(targetDateStr)) {
                return java.util.Collections.emptyList();
            }
        }

        // ── Parse openTime / closeTime ────────────────────────────────────────
        java.time.LocalTime open  = parseShopTime(shop.getOpenTime(),  java.time.LocalTime.of(8, 0));
        java.time.LocalTime close = parseShopTime(shop.getCloseTime(), java.time.LocalTime.of(20, 0));

        // ── Lấy danh sách staff active ────────────────────────────────────────
        List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shopId);

        // Nếu shop không có nhân viên nào → không có slot nào available
        if (activeStaff.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // ── Sinh slots và lọc ─────────────────────────────────────────────────
        List<LocalDateTime> availableSlots = new java.util.ArrayList<>();
        java.time.LocalTime cursor = open;

        while (!cursor.plusMinutes(durationMinutes).isAfter(close)) {
            LocalDateTime slotStart = LocalDateTime.of(date, cursor);
            LocalDateTime slotEnd   = slotStart.plusMinutes(durationMinutes);

            // Kiểm tra xem có ít nhất 1 staff rảnh trong slot này không
            boolean hasAvailableStaff = activeStaff.stream().anyMatch(s -> {
                // Check DB: có booking trùng giờ không?
                boolean dbBusy = bookingRepository.countConflictingBookingForStaff(
                        s.getId(), slotStart, slotEnd) > 0;
                if (dbBusy) return false;

                // Check Redis: slot đang bị giữ bởi pending booking không?
                String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                        + slotStart.withSecond(0).withNano(0).toString();
                boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
                return !redisBusy;
            });

            if (hasAvailableStaff) {
                availableSlots.add(slotStart);
            }

            cursor = cursor.plusMinutes(durationMinutes);
        }

        return availableSlots;
    }

    /**
     * Parse time string "HH:mm" hoặc "H:mm" thành LocalTime.
     * Nếu null / blank / lỗi → trả về defaultTime.
     */
    private java.time.LocalTime parseShopTime(String timeStr, java.time.LocalTime defaultTime) {
        if (timeStr == null || timeStr.isBlank()) return defaultTime;
        try {
            return java.time.LocalTime.parse(timeStr,
                    java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception e) {
            try {
                return java.time.LocalTime.parse(timeStr);
            } catch (Exception ex) {
                log.warn("Cannot parse shop time '{}', using default {}", timeStr, defaultTime);
                return defaultTime;
            }
        }
    }

    // ─── Available slots by service list ─────────────────────────────────────

    /**
     * Tính tổng duration của danh sách services, sinh slots theo bước 60 phút,
     * check conflict với tổng duration đó.
     *
     * Ví dụ: service A (60 phút) + service B (90 phút) = 150 phút tổng.
     * Slots: 08:00, 09:00, 10:00, ...
     * Slot 09:00 available khi có staff rảnh từ 09:00 → 11:30.
     *
     * Nếu staff đang có booking 09:30–10:30 (60 phút), thì:
     *   - Slot 08:00 (kết thúc 10:30): overlap → busy
     *   - Slot 09:00 (kết thúc 11:30): overlap → busy
     *   - Slot 10:00 (kết thúc 12:10): overlap → busy
     *   - Slot 11:00 (kết thúc 13:30): không overlap → available
     */
    @Override
    public List<LocalDateTime> getAvailableTimeSlotsForServices(int shopId,
                                                                  java.time.LocalDate date,
                                                                  List<Integer> serviceIds) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // ── Kiểm tra ngày nghỉ (Off Days) ─────────────────────────────────────
        if (shop.getOffDays() != null && !shop.getOffDays().trim().isEmpty()) {
            String targetDateStr = date.toString();
            if (java.util.Arrays.asList(shop.getOffDays().split(",")).contains(targetDateStr)) {
                return java.util.Collections.emptyList();
            }
        }

        // ── Tính tổng duration ────────────────────────────────────────────────
        int totalDuration = serviceIds.stream()
                .mapToInt(id -> serviceRepository.findById(id)
                        .map(Service::getDurationMinutes)
                        .orElse(0))
                .sum();

        if (totalDuration <= 0) totalDuration = 60; // fallback

        // ── Parse openTime / closeTime ────────────────────────────────────────
        java.time.LocalTime open  = parseShopTime(shop.getOpenTime(),  java.time.LocalTime.of(8, 0));
        java.time.LocalTime close = parseShopTime(shop.getCloseTime(), java.time.LocalTime.of(20, 0));

        // ── Lấy danh sách staff active ────────────────────────────────────────
        List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shopId);
        if (activeStaff.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // ── Sinh slots theo bước 60 phút, check conflict với totalDuration ────
        // Bước 60 phút để UI hiển thị đẹp (08:00, 09:00, 10:00...)
        // Nhưng window check = totalDuration để đảm bảo đủ thời gian làm hết services
        final int SLOT_STEP = 60;
        final int finalTotalDuration = totalDuration;

        List<LocalDateTime> availableSlots = new java.util.ArrayList<>();
        java.time.LocalTime cursor = open;

        while (!cursor.plusMinutes(finalTotalDuration).isAfter(close)) {
            LocalDateTime slotStart = LocalDateTime.of(date, cursor);
            LocalDateTime slotEnd   = slotStart.plusMinutes(finalTotalDuration);

            boolean hasAvailableStaff = activeStaff.stream().anyMatch(s -> {
                boolean dbBusy = bookingRepository.countConflictingBookingForStaff(
                        s.getId(), slotStart, slotEnd) > 0;
                if (dbBusy) return false;
                String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                        + slotStart.withSecond(0).withNano(0).toString();
                return !Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
            });

            if (hasAvailableStaff) {
                availableSlots.add(slotStart);
            }

            cursor = cursor.plusMinutes(SLOT_STEP);
        }

        return availableSlots;
    }
    @Override
    @Transactional
    public BookingResponse mockConfirmCashDeposit(long orderCode, String userEmail) {
        User user = resolveUser(userEmail);

        // Idempotent check
        bookingRepository.findByPayosOrderCode(orderCode).ifPresent(existing -> {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
        });

        // Lấy pending từ Redis
        Object raw = redisTemplate.opsForValue().get(CASH_PENDING_PREFIX + orderCode);
        if (raw == null) {
            log.warn("Cash pending booking not found in Redis for orderCode={}", orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }
        PendingBooking pending = (raw instanceof PendingBooking pb) ? pb : null;
        if (pending == null) throw new AppException(ErrorCode.BOOKING_NOT_FOUND);

        if (pending.getUserId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        // ── Kiểm tra lại trùng lịch ───────────────────────────────────────────
        List<Integer> pendingServiceIds = resolveServiceIds(pending.getServiceId(), pending.getServiceIds());
        int pendingTotalDuration = resolveTotalDuration(pendingServiceIds);
        Service service = serviceRepository.findById(pending.getServiceId()).get();
        boolean isBoarding = "BOARDING".equalsIgnoreCase(service.getCategory()) || "Hotel".equalsIgnoreCase(service.getCategory());
        checkPetConflict(pending.getPetId(), pending.getShopId(), pending.getAppointmentDatetime(), pendingTotalDuration, isBoarding);

        if (pending.getStaffId() != null) {
            LocalDateTime ws = pending.getAppointmentDatetime();
            LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
            if (bookingRepository.countConflictingBookingForStaff(pending.getStaffId(), ws, we) > 0) {
                redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
                releaseStaffSlot(pending.getStaffId(), pending.getAppointmentDatetime());
                throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
            }
        }

        Shop  shop = shopRepository.findById(pending.getShopId()).get();
        Pet   pet  = petRepository.findById(pending.getPetId()).get();
        Staff staff = pending.getStaffId() != null
                ? staffRepository.findById(pending.getStaffId()).orElse(null)
                : null;

        if (staff == null) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = pending.getAppointmentDatetime();
                LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(pendingTotalDuration);
                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = bookingRepository.countConflictingBookingForStaff(s.getId(), ws, we) > 0;
                    String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                            + pending.getAppointmentDatetime().withSecond(0).withNano(0).toString();
                    boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
                    return !dbBusy && !redisBusy;
                }).collect(Collectors.toList());
                if (!availableStaff.isEmpty()) {
                    staff = availableStaff.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(availableStaff.size()));
                } else {
                    staff = activeStaff.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(activeStaff.size()));
                }
            }
        }

        // ── Tạo Booking ───────────────────────────────────────────────────────
        java.util.Set<Service> servicesSet = pendingServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Booking booking = Booking.builder()
                .user(user).shop(shop).pet(pet).staff(staff)
                .services(servicesSet)
                .appointmentDatetime(pending.getAppointmentDatetime())
                .checkIn(pending.getCheckIn())
                .checkOut(pending.getCheckOut())
                .note(pending.getNote())
                .status((staff != null && !"AUTO".equals(shop.getAssignmentMode())) ? "WAITING_SHOP_APPROVAL" : "CONFIRMED")
                .payosOrderCode(orderCode)
                .build();
        booking = bookingRepository.save(booking);

        // ── Tạo Payment: ghi nhận tiền cọc (MOCK) ───────
        BigDecimal depositAmount = new BigDecimal(pending.getAmountVnd());
        String fullPriceStr = servicesSet.stream().map(s -> s.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString();
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(depositAmount)
                .method("MOCK_CASH_DEPOSIT")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .description(String.format(
                        "Tiền cọc (MOCK) cho đơn thu tiền mặt #%d — tổng giá: %s VND",
                        booking.getId(), fullPriceStr))
                .gatewayTransactionId("MOCK-" + orderCode)
                .build();
        paymentRepository.save(payment);

        // ── Ghi Transaction: tiền cọc ─────────────────────────────────────
        transactionRepository.save(Transaction.builder()
                .booking(booking)
                .shop(shop)
                .type("BOOKING_PAYMENT")
                .amount(depositAmount)
                .paymentMethod("MOCK_CASH_DEPOSIT")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .gatewayTransactionId("MOCK-" + orderCode)
                .description(String.format(
                        "Đã thanh toán cọc (MOCK) cho đơn #%d", booking.getId()))
                .completedAt(LocalDateTime.now())
                .build());

        redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
        if (staff != null) {
            releaseStaffSlot(staff.getId(), pending.getAppointmentDatetime());
        }

        // --- Notification cho Shop Owner và Staff ---
        Notification notifOwner = Notification.builder()
                .user(shop.getOwner())
                .title("Có đơn đặt lịch mới (Cọc tiền mặt - MOCK)")
                .content(String.format("Có đơn đặt lịch mới #%d từ khách hàng %s.", booking.getId(), user.getFullName()))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifOwner);
        messagingTemplate.convertAndSend("/topic/shop/" + shop.getId() + "/notifications", java.util.Map.of("message", "Có đơn hàng mới!"));

        if (staff != null && staff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(staff.getUser())
                    .title("Bạn vừa được giao lịch hẹn mới")
                    .content(String.format("Bạn vừa được giao một lịch hẹn mới #%d.", booking.getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }

        log.info("Booking {} CONFIRMED (MOCK CASH) — orderCode={}", booking.getId(), orderCode);
        return toResponse(booking);
    }

    // ─── Send invoice manually ────────────────────────────────────────────────

    @Override
    public void sendInvoice(int bookingId, String requesterEmail) {
        // Xác định requester là shop owner
        User requester = resolveUser(requesterEmail);

        Booking booking = bookingRepository.findByIdWithServices(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Kiểm tra quyền: chỉ shop owner của shop đó
        boolean isOwner = requester.getRoles().stream().anyMatch(r -> "SHOP_OWNER".equals(r.getName()))
                && booking.getShop().getOwner() != null
                && booking.getShop().getOwner().getId() == requester.getId();

        if (!isOwner) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Lấy thông tin payment
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        String method = payment != null ? payment.getMethod() : "CASH";

        // Tính số tiền đã thanh toán thực tế
        BigDecimal paidAmount;
        if (payment != null && payment.getAmount() != null) {
            if ("CASH_DEPOSIT".equals(method) || "MOCK_CASH_DEPOSIT".equals(method)) {
                // Cash deposit: full price = tổng giá dịch vụ
                paidAmount = (booking.getServices() != null)
                        ? booking.getServices().stream()
                                .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                        : payment.getAmount();
            } else {
                paidAmount = payment.getAmount();
            }
        } else {
            paidAmount = (booking.getServices() != null)
                    ? booking.getServices().stream()
                            .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
        }

        // Xác định nhãn phương thức thanh toán
        String invoiceMethod = (method != null) ? method.replace("MOCK_", "") : "CASH";

        String customerEmail = booking.getUser().getEmail();
        emailService.sendBookingInvoiceEmail(
                customerEmail,
                booking,
                paidAmount,
                invoiceMethod,
                "Đã thanh toán thành công"
        );

        log.info("Invoice for booking {} sent to {} by shop owner {}", bookingId, customerEmail, requesterEmail);
    }

    @Override
    @Transactional
    public BookingResponse createBookingByShop(com.sang.sourcepattern.dto.request.ShopCreateBookingRequest request, String requesterEmail) {
        User requester = resolveUser(requesterEmail);
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Check permission
        boolean isOwner = shop.getOwner().getId() == requester.getId();
        boolean isStaff = staffRepository.findByUserEmail(requesterEmail)
                .map(s -> s.getShop().getId() == shop.getId())
                .orElse(false);

        if (!isOwner && !isStaff) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Integer> effectiveServiceIds = request.getServiceIds();
        if (effectiveServiceIds == null || effectiveServiceIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        int totalDuration = resolveTotalDuration(effectiveServiceIds);

        boolean isBoarding = effectiveServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .anyMatch(s -> s != null && ("BOARDING".equalsIgnoreCase(s.getCategory()) || "Hotel".equalsIgnoreCase(s.getCategory())));

        checkPetConflict(request.getPetId(), request.getShopId(), request.getAppointmentDatetime(), totalDuration, isBoarding);

        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(), totalDuration, isBoarding);
        } else if (!isBoarding) {
            checkShopHasAvailableStaff(request.getShopId(), request.getAppointmentDatetime(), totalDuration, isBoarding);
        }

        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_EXISTED));

        if (pet.getOwner().getId() != customer.getId()) {
            throw new AppException(ErrorCode.PET_NOT_BELONG_TO_USER);
        }

        Staff staff = request.getStaffId() != null
                ? staffRepository.findById(request.getStaffId()).orElse(null)
                : null;

        if (staff == null && !isBoarding) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = request.getAppointmentDatetime();
                LocalDateTime we = request.getAppointmentDatetime().plusMinutes(totalDuration);

                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = bookingRepository.countConflictingBookingForStaff(s.getId(), ws, we) > 0;
                    String slotKey = STAFF_SLOT_PREFIX + s.getId() + ":"
                                 + request.getAppointmentDatetime().withSecond(0).withNano(0).toString();
                    boolean redisBusy = Boolean.TRUE.equals(redisTemplate.hasKey(slotKey));
                    return !dbBusy && !redisBusy;
                }).collect(Collectors.toList());

                if (!availableStaff.isEmpty()) {
                    staff = availableStaff.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(availableStaff.size()));
                } else {
                    staff = activeStaff.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(activeStaff.size()));
                }
            }
        }

        java.util.Set<Service> servicesSet = effectiveServiceIds.stream()
                .map(id -> serviceRepository.findById(id).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Booking booking = Booking.builder()
                .user(customer)
                .shop(shop)
                .pet(pet)
                .staff(staff)
                .services(servicesSet)
                .appointmentDatetime(request.getAppointmentDatetime())
                .checkOutDatetime(request.getCheckOut())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .petWeight(request.getPetWeight())
                .roomType(request.getRoomType())
                .note(request.getNote())
                .status("CONFIRMED")
                .createdBy("SHOP")
                .build();

        booking = bookingRepository.save(booking);

        Notification notifUser = Notification.builder()
                .user(customer)
                .title("Có đơn đặt lịch mới từ shop")
                .content(String.format("Shop %s đã tạo đơn đặt lịch mới #%d cho bạn.", shop.getShopName(), booking.getId()))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifUser);

        if (staff != null && staff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(staff.getUser())
                    .title("Bạn vừa được giao lịch hẹn mới")
                    .content(String.format("Bạn vừa được giao một lịch hẹn mới #%d.", booking.getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }

        log.info("Booking {} CONFIRMED (Created by Shop) — customer={}", booking.getId(), customer.getEmail());

        return toResponse(booking);
    }

    private void sendBookingUpdateEvent(Integer shopId, Integer bookingId) {
        if (shopId != null && messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/shop/" + shopId + "/notifications", 
                java.util.Map.of("message", "BOOKING_UPDATED", "bookingId", bookingId));
        }
    }
}
