package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.BookingCreationRequest;
import com.sang.sourcepattern.dto.request.InitiatePaymentRequest;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.InitiatePaymentResponse;
import com.sang.sourcepattern.dto.response.PayOSCreateResponse;
import com.sang.sourcepattern.dto.response.PayOSPaymentInfoResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.Payment;
import com.sang.sourcepattern.entity.Pet;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.Staff;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.entity.Service;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingServiceImpl implements BookingService {

    BookingRepository  bookingRepository;
    PaymentRepository  paymentRepository;
    UserRepository     userRepository;
    ShopRepository     shopRepository;
    ServiceRepository  serviceRepository;
    PetRepository      petRepository;
    StaffRepository    staffRepository;
    PayOSService       payOSService;

    @Value("${payos.return-url}") @NonFinal String returnUrl;
    @Value("${payos.cancel-url}") @NonFinal String cancelUrl;

    /**
     * Temporary in-memory store: orderCode → pending booking data.
     * Cleared after confirmPayment succeeds or expires.
     */
    Map<Long, PendingBooking> pendingBookings = new ConcurrentHashMap<>();

    /** Holds all data needed to create a booking after payment confirmed */
    @Data @AllArgsConstructor
    static class PendingBooking {
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

    // ─── helpers ─────────────────────────────────────────────────────────────

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

    // ─── STEP 1: Initiate PayOS payment (no booking saved) ───────────────────

    @Override
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userEmail) {
        User user = resolveUser(userEmail);

        validateBookingInputs(request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(), user.getId());

        Service service = serviceRepository.findById(request.getServiceId()).get();

        long orderCode = ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
        String description = "Booking" + orderCode % 100000; // short, alphanumeric
        int rawAmount = service.getPrice().intValue();
        int amountVnd = Math.max(rawAmount, 2000);
        if (amountVnd % 1000 != 0) amountVnd = ((amountVnd / 1000) + 1) * 1000;

        // Store pending data — will be used in confirmPayment
        pendingBookings.put(orderCode, new PendingBooking(
                user.getId(), request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(),
                request.getAppointmentDatetime(), request.getNote(),
                amountVnd, description
        ));

        PayOSCreateResponse payosResponse;
        try {
            payosResponse = payOSService.createPaymentLink(
                    orderCode, amountVnd, description, returnUrl, cancelUrl);
        } catch (Exception e) {
            pendingBookings.remove(orderCode);
            log.error("PayOS createPaymentLink failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_ERROR);
        }

        if (payosResponse == null || !payosResponse.isSuccess() || payosResponse.getData() == null) {
            pendingBookings.remove(orderCode);
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

        // Check if already confirmed (idempotent)
        bookingRepository.findByPayosOrderCode(orderCode).ifPresent(existing -> {
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);
        });

        // Query PayOS for real status
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
            // Not paid — clean up pending and return error
            pendingBookings.remove(orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND); // reuse as "payment not completed"
        }

        // Retrieve pending booking data
        PendingBooking pending = pendingBookings.get(orderCode);
        if (pending == null) {
            // Pending data expired (server restart) — still create booking from PayOS data
            log.warn("Pending booking data not found for orderCode={}, cannot create booking", orderCode);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (pending.getUserId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        // Create booking entities
        Shop shop = shopRepository.findById(pending.getShopId()).get();
        Service service = serviceRepository.findById(pending.getServiceId()).get();
        Pet pet = petRepository.findById(pending.getPetId()).get();
        Staff staff = pending.getStaffId() != null
                ? staffRepository.findById(pending.getStaffId()).orElse(null)
                : null;

        // Auto-assign if mode is AUTO and no staff selected
        if (staff == null && "AUTO".equals(shop.getAssignmentMode())) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                staff = activeStaff.get(ThreadLocalRandom.current().nextInt(activeStaff.size()));
            }
        }

        Booking booking = Booking.builder()
                .user(user).shop(shop).service(service).pet(pet).staff(staff)
                .appointmentDatetime(pending.getAppointmentDatetime())
                .note(pending.getNote())
                .status("CONFIRMED")
                .payosOrderCode(orderCode)
                .build();
        booking = bookingRepository.save(booking);

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

        pendingBookings.remove(orderCode);

        log.info("Booking {} CONFIRMED after PayOS payment — orderCode={}", booking.getId(), orderCode);
        return toResponse(booking);
    }

    // ─── CASH booking ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponse createCashBooking(BookingCreationRequest request, String userEmail) {
        User user = resolveUser(userEmail);

        validateBookingInputs(request.getShopId(), request.getServiceId(),
                request.getPetId(), request.getStaffId(), user.getId());

        Shop shop = shopRepository.findById(request.getShopId()).get();
        Service service = serviceRepository.findById(request.getServiceId()).get();
        Pet pet = petRepository.findById(request.getPetId()).get();
        Staff staff = request.getStaffId() != null
                ? staffRepository.findById(request.getStaffId()).orElse(null)
                : null;

        // Auto-assign if mode is AUTO and no staff selected
        if (staff == null && "AUTO".equals(shop.getAssignmentMode())) {
            List<Staff> activeStaff = staffRepository.findByShopIdAndIsActiveTrue(shop.getId());
            if (!activeStaff.isEmpty()) {
                staff = activeStaff.get(ThreadLocalRandom.current().nextInt(activeStaff.size()));
            }
        }

        Booking booking = Booking.builder()
                .user(user).shop(shop).service(service).pet(pet).staff(staff)
                .appointmentDatetime(request.getAppointmentDatetime())
                .note(request.getNote())
                .status("CONFIRMED")
                .build();
        booking = bookingRepository.save(booking);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(service.getPrice())
                .method("CASH")
                .status("PENDING")
                .description("Cash payment for booking " + booking.getId())
                .build();
        paymentRepository.save(payment);

        log.info("Booking {} created (CASH) — status=CONFIRMED", booking.getId());
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

        boolean isUser = user.getRoles().stream().anyMatch(r -> "USER".equals(r.getName()));
        boolean isOwner = user.getRoles().stream().anyMatch(r -> "SHOP_OWNER".equals(r.getName()));

        if (isUser && booking.getUser().getId() != user.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        if (isOwner) {
            Shop shop = shopRepository.findByOwnerEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
            if (booking.getShop().getId() != shop.getId())
                throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        }

        if (!isUser && !isOwner) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if ("COMPLETED".equals(booking.getStatus()))
            throw new AppException(ErrorCode.BOOKING_ALREADY_PAID);

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            p.setStatus("CANCELLED");
            paymentRepository.save(p);
        });

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
}
