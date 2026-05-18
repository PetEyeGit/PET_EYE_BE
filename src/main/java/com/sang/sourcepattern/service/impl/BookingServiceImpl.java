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
import java.time.LocalDateTime;
import java.util.List;
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
                .payosOrderCode(booking.getPayosOrderCode())
                .createdAt(booking.getCreatedAt())
                .checkoutUrl(payment != null ? payment.getCheckoutUrl() : null)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .build();
    }

    /**
     * Kiểm tra pet có booking trùng giờ không.
     * Window = [appointmentTime - duration, appointmentTime + duration]
     * để tránh overlap khi dịch vụ có thời lượng.
     */
    private void checkPetConflict(int petId, LocalDateTime appointmentTime, int durationMinutes) {
        LocalDateTime windowStart = appointmentTime.minusMinutes(durationMinutes);
        LocalDateTime windowEnd   = appointmentTime.plusMinutes(durationMinutes);

        boolean conflict = bookingRepository.existsConflictingBookingForPet(
                petId, windowStart, windowEnd);

        if (conflict) {
            throw new AppException(ErrorCode.PET_BOOKING_CONFLICT);
        }
    }

    /**
     * Kiểm tra staff có bị trùng lịch không.
     * Kiểm tra cả DB (booking đã xác nhận) lẫn Redis (pending booking đang chờ thanh toán).
     */
    private void checkStaffConflict(int staffId, LocalDateTime appointmentTime, int durationMinutes) {
        // 1. Kiểm tra DB — booking đã CONFIRMED/IN_PROGRESS
        LocalDateTime windowStart = appointmentTime.minusMinutes(durationMinutes);
        LocalDateTime windowEnd   = appointmentTime.plusMinutes(durationMinutes);

        boolean dbConflict = bookingRepository.existsConflictingBookingForStaff(
                staffId, windowStart, windowEnd);
        if (dbConflict) {
            throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
        }

        // 2. Kiểm tra Redis — pending booking đang giữ slot (chưa thanh toán)
        String slotKey = staffSlotKey(staffId, appointmentTime);
        Boolean slotTaken = redisTemplate.hasKey(slotKey);
        if (Boolean.TRUE.equals(slotTaken)) {
            throw new AppException(ErrorCode.STAFF_BOOKING_CONFLICT);
        }
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

        validateBookingInputs(request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(), user.getId());

        Service service = serviceRepository.findById(request.getServiceId()).get();

        // ── Kiểm tra trùng lịch pet ──────────────────────────────────────────
        checkPetConflict(request.getPetId(), request.getAppointmentDatetime(),
                service.getDurationMinutes());

        // ── Kiểm tra trùng lịch staff (nếu có chọn staff) ────────────────────
        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(),
                    service.getDurationMinutes());
        }

        long orderCode = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Booking" + orderCode % 100000;
        int rawAmount = service.getPrice().intValue();
        int amountVnd = Math.max(rawAmount, 2000);
        if (amountVnd % 1000 != 0) amountVnd = ((amountVnd / 1000) + 1) * 1000;

        // ── Lưu vào Redis (TTL 30 phút) ──────────────────────────────────────
        PendingBooking pending = new PendingBooking(
                user.getId(), request.getShopId(), request.getServiceId(),
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
        Service service = serviceRepository.findById(pending.getServiceId()).get();
        checkPetConflict(pending.getPetId(), pending.getAppointmentDatetime(),
                service.getDurationMinutes());

        // Double-check staff conflict từ DB (slot Redis đã được giữ ở bước 1)
        if (pending.getStaffId() != null) {
            LocalDateTime ws = pending.getAppointmentDatetime().minusMinutes(service.getDurationMinutes());
            LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(service.getDurationMinutes());
            if (bookingRepository.existsConflictingBookingForStaff(pending.getStaffId(), ws, we)) {
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

        if (staff == null && "AUTO".equals(shop.getAssignmentMode())) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = pending.getAppointmentDatetime().minusMinutes(service.getDurationMinutes());
                LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(service.getDurationMinutes());
                
                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = bookingRepository.existsConflictingBookingForStaff(s.getId(), ws, we);
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
                .status("CONFIRMED")
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

        validateBookingInputs(request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(), user.getId());

        Service service = serviceRepository.findById(request.getServiceId()).get();

        // ── Kiểm tra trùng lịch pet ──────────────────────────────────────────
        checkPetConflict(request.getPetId(), request.getAppointmentDatetime(),
                service.getDurationMinutes());

        // ── Kiểm tra trùng lịch staff ─────────────────────────────────────────
        if (request.getStaffId() != null) {
            checkStaffConflict(request.getStaffId(), request.getAppointmentDatetime(),
                    service.getDurationMinutes());
        }

        // ── Tính tiền cọc = 10% giá dịch vụ (= phí hoa hồng admin) ──────────
        BigDecimal depositRate = walletService.getAdminFeeRate();
        BigDecimal rawDeposit  = service.getPrice().multiply(depositRate);
        int depositVnd = rawDeposit.setScale(0, java.math.RoundingMode.CEILING).intValue();
        // PayOS yêu cầu tối thiểu 2000 VND và chia hết 1000
        depositVnd = Math.max(depositVnd, 2000);
        if (depositVnd % 1000 != 0) depositVnd = ((depositVnd / 1000) + 1) * 1000;

        long orderCode  = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Deposit" + orderCode % 100000;

        // ── Lưu vào Redis (TTL 30 phút) — tái dùng PendingBooking ────────────
        PendingBooking pending = new PendingBooking(
                user.getId(), request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(),
                request.getAppointmentDatetime(), request.getNote(),
                depositVnd, description
        );
        // Đánh dấu đây là cash booking bằng cách prefix key
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
            log.error("PayOS createPaymentLink (cash deposit) failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        if (payosResponse == null || !payosResponse.isSuccess() || payosResponse.getData() == null) {
            redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
            if (request.getStaffId() != null) {
                releaseStaffSlot(request.getStaffId(), request.getAppointmentDatetime());
            }
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        log.info("Cash deposit PayOS link created — orderCode={} deposit={}VND (10% of {})",
                orderCode, depositVnd, service.getPrice());

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

        // Query PayOS
        PayOSPaymentInfoResponse info;
        try {
            info = payOSService.getPaymentInfo(orderCode);
        } catch (Exception e) {
            log.error("PayOS getPaymentInfo (cash deposit) failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        String payosStatus = info.getPaymentStatus();
        log.info("PayOS confirmCashDeposit — orderCode={} status={}", orderCode, payosStatus);

        if (!"PAID".equals(payosStatus)) {
            redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

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
        Service service = serviceRepository.findById(pending.getServiceId()).get();
        checkPetConflict(pending.getPetId(), pending.getAppointmentDatetime(),
                service.getDurationMinutes());

        if (pending.getStaffId() != null) {
            LocalDateTime ws = pending.getAppointmentDatetime().minusMinutes(service.getDurationMinutes());
            LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(service.getDurationMinutes());
            if (bookingRepository.existsConflictingBookingForStaff(pending.getStaffId(), ws, we)) {
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

        if (staff == null && "AUTO".equals(shop.getAssignmentMode())) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                LocalDateTime ws = pending.getAppointmentDatetime().minusMinutes(service.getDurationMinutes());
                LocalDateTime we = pending.getAppointmentDatetime().plusMinutes(service.getDurationMinutes());
                List<Staff> availableStaff = activeStaff.stream().filter(s -> {
                    boolean dbBusy = bookingRepository.existsConflictingBookingForStaff(s.getId(), ws, we);
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
                .status("CONFIRMED")
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
                .gatewayTransactionId(info.getData() != null ? info.getData().getId() : null)
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
                .gatewayTransactionId(info.getData() != null ? info.getData().getId() : null)
                .description(String.format(
                        "10%% deposit paid (PayOS) for cash booking #%d", booking.getId()))
                .completedAt(LocalDateTime.now())
                .build());

        redisTemplate.delete(CASH_PENDING_PREFIX + orderCode);
        if (staff != null) {
            releaseStaffSlot(staff.getId(), pending.getAppointmentDatetime());
        }

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

        if ("COMPLETED".equals(booking.getStatus()))
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

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
                        LocalDateTime ws = appointmentDatetime.minusMinutes(durationMinutes);
                        LocalDateTime we = appointmentDatetime.plusMinutes(durationMinutes);

                        // Check DB
                        boolean dbBusy = bookingRepository.existsConflictingBookingForStaff(s.getId(), ws, we);

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
    public List<BookingResponse> getShopBookings(String ownerEmail, LocalDateTime start, LocalDateTime end) {
        Shop shop = shopRepository.findByOwnerEmail(ownerEmail)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        List<Booking> bookings = (start != null && end != null)
                ? bookingRepository.findByShopIdAndAppointmentDatetimeBetween(shop.getId(), start, end)
                : bookingRepository.findByShopId(shop.getId());

        return bookings.stream().map(this::toResponse).collect(Collectors.toList());
    }
}
