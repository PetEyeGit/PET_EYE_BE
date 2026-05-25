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
import com.sang.sourcepattern.service.impl.WalletServiceImpl;
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
    /** Redis template để lưu PendingBooking thay vì in-memory */
    RedisTemplate<String, Object> redisTemplate;

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
        String note;
        int amountVnd;
        String description;
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
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .shopId(booking.getShop().getId())
                .shopName(booking.getShop().getShopName())
                .shopAddress(booking.getShop().getAddress())
                .serviceId(booking.getService().getId())
                .serviceName(booking.getService().getServiceName())
                .servicePrice(booking.getService().getPrice())
                .petId(booking.getPet().getId())
                .petName(booking.getPet().getName())
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
                .checkoutUrl(payment != null ? payment.getCheckoutUrl() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .build();
    }

    /**
     * Kiểm tra pet có booking trùng giờ không.
     * Dùng interval overlap: newStart < existingEnd AND existingStart < newEnd
     * windowStart = appointmentTime (bắt đầu booking mới)
     * windowEnd   = appointmentTime + durationMinutes (kết thúc booking mới)
     */
    private void checkPetConflict(int petId, LocalDateTime appointmentTime, int durationMinutes) {
        LocalDateTime windowStart = appointmentTime;
        LocalDateTime windowEnd   = appointmentTime.plusMinutes(durationMinutes);

        boolean conflict = bookingRepository.countConflictingBookingForPet(
                petId, windowStart, windowEnd) > 0;

        if (conflict) {
            throw new AppException(ErrorCode.PET_BOOKING_CONFLICT);
        }
    }

    /**
     * Kiểm tra staff có bị trùng lịch không.
     * Dùng interval overlap: newStart < existingEnd AND existingStart < newEnd
     */
    private void checkStaffConflict(int staffId, LocalDateTime appointmentTime, int durationMinutes) {
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
    private void checkShopHasAvailableStaff(int shopId, LocalDateTime appointmentTime, int durationMinutes) {
        List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shopId);
        if (activeStaff.isEmpty()) {
            throw new AppException(ErrorCode.NO_STAFF_AVAILABLE);
        }

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

    /**
     * Tính tổng giá của danh sách services.
     */
    private java.math.BigDecimal resolveTotalPrice(List<Integer> ids) {
        return ids.stream()
                .map(id -> serviceRepository.findById(id)
                        .map(Service::getPrice)
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

        // ── Kiểm tra trùng lịch pet ──────────────────────────────────────────
        checkPetConflict(request.getPetId(), request.getAppointmentDatetime(), totalDuration);

        // ── Kiểm tra trùng lịch staff (nếu có chọn staff) ────────────────────
        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(), totalDuration);
        } else {
            // Không chọn staff cụ thể → kiểm tra shop còn nhân viên rảnh không
            checkShopHasAvailableStaff(request.getShopId(), request.getAppointmentDatetime(), totalDuration);
        }

        long orderCode = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Booking" + orderCode % 100000;
        // Tổng giá tất cả services
        int rawAmount = resolveTotalPrice(effectiveServiceIds).intValue();
        int amountVnd = Math.max(rawAmount, 2000);
        if (amountVnd % 1000 != 0) amountVnd = ((amountVnd / 1000) + 1) * 1000;

        // ── Lưu vào Redis (TTL 30 phút) ──────────────────────────────────────
        PendingBooking pending = new PendingBooking(
                user.getId(), request.getShopId(), request.getServiceId(),
                effectiveServiceIds,
                request.getPetId(), request.getStaffId(),
                request.getAppointmentDatetime(), request.getNote(),
                amountVnd, description
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

        checkPetConflict(pending.getPetId(), pending.getAppointmentDatetime(), pendingTotalDuration);

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

        // ── Tạo Booking ───────────────────────────────────────────────────────
        Booking booking = Booking.builder()
                .user(user).shop(shop).service(service).pet(pet).staff(staff)
                .appointmentDatetime(pending.getAppointmentDatetime())
                .note(pending.getNote())
                .status((staff != null && !"AUTO".equals(shop.getAssignmentMode())) ? "WAITING_SHOP_APPROVAL" : "CONFIRMED")
                .payosOrderCode(orderCode)
                .build();
        booking = bookingRepository.save(booking);

        // ── Tạo Payment ───────────────────────────────────────────────────────
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(service.getPrice())
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
                .amount(service.getPrice())
                .paymentMethod("PAYOS")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .gatewayTransactionId(info.getData() != null ? info.getData().getId() : null)
                .description("PayOS payment for booking #" + booking.getId())
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

        // ── Kiểm tra trùng lịch pet ──────────────────────────────────────────
        checkPetConflict(request.getPetId(), request.getAppointmentDatetime(), totalDuration);

        // ── Kiểm tra trùng lịch staff ─────────────────────────────────────────
        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(), totalDuration);
        } else {
            // Không chọn staff cụ thể → kiểm tra shop còn nhân viên rảnh không
            checkShopHasAvailableStaff(request.getShopId(), request.getAppointmentDatetime(), totalDuration);
        }

        // ── Tính tiền cọc = 10% tổng giá dịch vụ ────────────────────────────
        BigDecimal depositRate = walletService.getAdminFeeRate();
        BigDecimal totalPrice  = resolveTotalPrice(effectiveServiceIds);
        BigDecimal rawDeposit  = totalPrice.multiply(depositRate);
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
                request.getAppointmentDatetime(), request.getNote(),
                depositVnd, description
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
        checkPetConflict(pending.getPetId(), pending.getAppointmentDatetime(), pendingTotalDuration);

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
                    staff = availableStaff.get(ThreadLocalRandom.current().nextInt(availableStaff.size()));
                } else {
                    staff = activeStaff.get(ThreadLocalRandom.current().nextInt(activeStaff.size()));
                }
            }
        }

        // ── Tạo Booking ───────────────────────────────────────────────────────
        Booking booking = Booking.builder()
                .user(user).shop(shop).service(service).pet(pet).staff(staff)
                .appointmentDatetime(pending.getAppointmentDatetime())
                .note(pending.getNote())
                .status((staff != null && !"AUTO".equals(shop.getAssignmentMode())) ? "WAITING_SHOP_APPROVAL" : "CONFIRMED")
                .payosOrderCode(orderCode)
                .build();
        booking = bookingRepository.save(booking);

        // ── Tạo Payment: ghi nhận tiền cọc 10% đã thanh toán qua PayOS ───────
        BigDecimal depositAmount = new BigDecimal(pending.getAmountVnd());
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(depositAmount)
                .method("CASH_DEPOSIT")
                .status("SUCCESS")
                .payosOrderCode(orderCode)
                .description(String.format(
                        "10%% deposit (PayOS) for cash booking #%d — full price: %s VND",
                        booking.getId(), service.getPrice().toPlainString()))
                .gatewayTransactionId(gatewayTxId)
                .build();
        paymentRepository.save(payment);

        // ── Ghi Transaction: tiền cọc 10% ─────────────────────────────────────
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
                        "10%% deposit paid (PayOS) for cash booking #%d", booking.getId()))
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

        log.info("Cash booking {} CONFIRMED — deposit={}VND (10%), remaining {}VND (90%) to be collected in cash",
                booking.getId(), depositAmount, service.getPrice().subtract(depositAmount));
        return toResponse(booking);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Override
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = resolveUser(userEmail);
        return bookingRepository.findByUserId(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public BookingResponse getBookingById(int bookingId, String userEmail) {
        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        if (booking.getUser().getId() != user.getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        return toResponse(booking);
    }

    // ─── Cancel ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponse cancelBooking(int bookingId, String userEmail) {
        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findById(bookingId)
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

        if (wasCancelRequested) {
            List<User> admins = userRepository.findByRoleName("ADMIN");
            String broadcastId = UUID.randomUUID().toString();
            String bankInfo = (booking.getBankName() != null && !booking.getBankName().isBlank())
                    ? String.format("%s | %s | %s", booking.getBankName(), booking.getAccountHolder(), booking.getBankAccount())
                    : "Chưa có thông tin ngân hàng";
            BigDecimal refundAmount = booking.getService() != null ? booking.getService().getPrice() : BigDecimal.ZERO;
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

        log.info("Booking {} CANCELLED by {}", bookingId, userEmail);
        return toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse requestBookingCancellation(int bookingId, String reason, String bankName, String bankAccount, String accountHolder, String userEmail) {
        if (reason == null || reason.isBlank() || bankName == null || bankName.isBlank() || bankAccount == null || bankAccount.isBlank() || accountHolder == null || accountHolder.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        User user = resolveUser(userEmail);
        Booking booking = bookingRepository.findById(bookingId)
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
        booking.setBankName(bankName);
        booking.setBankAccount(bankAccount);
        booking.setAccountHolder(accountHolder);
        bookingRepository.save(booking);

        log.info("Booking {} CANCEL REQUESTED by {}", bookingId, userEmail);
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
        return bookingRepository.countConflictingBookingForPet(petId, windowStart, windowEnd) == 0;
    }

    @Override
    public List<BookingResponse> getShopBookings(String ownerEmail, LocalDateTime start, LocalDateTime end) {
        Shop shop = shopRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        List<Booking> bookings = (start != null && end != null)
                ? bookingRepository.findByShopIdAndAppointmentDatetimeBetween(shop.getId(), start, end)
                : bookingRepository.findByShopId(shop.getId());

        return bookings.stream().map(this::toResponse).collect(Collectors.toList());
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
}
