package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.TaskStatusUpdateRequest;
import com.sang.sourcepattern.dto.response.TaskResponse;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.Staff;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.TaskService;
import com.sang.sourcepattern.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TaskServiceImpl implements TaskService {

    BookingRepository bookingRepository;
    StaffRepository   staffRepository;
    UserRepository    userRepository;
    ShopRepository    shopRepository;
    WalletService     walletService;

    // Valid status transition chain for Staff
    private static final Set<String> VALID_STATUSES = Set.of("CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED");

    // ─── helpers ──────────────────────────────────────────────────────────────

    private User resolveUser(String identifier) {
        if (identifier == null) throw new AppException(ErrorCode.USER_NOT_EXISTED);
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } else {
            try {
                return userRepository.findById(Integer.parseInt(identifier))
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.USER_NOT_EXISTED);
            }
        }
    }

    private Staff resolveStaffByEmail(String identifier) {
        User user = resolveUser(identifier);
        return staffRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    }

    private Shop resolveOwnerShop(String identifier) {
        if (identifier == null) throw new AppException(ErrorCode.SHOP_NOT_FOUND);
        if (identifier.contains("@")) {
            return shopRepository.findByOwnerEmail(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        } else {
            try {
                return shopRepository.findByOwnerId(Integer.parseInt(identifier))
                        .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
        }
    }

    private TaskResponse toResponse(Booking b) {
        return TaskResponse.builder()
                .bookingId(b.getId())
                .shopId(b.getShop().getId())
                .shopName(b.getShop().getShopName())
                .petId(b.getPet().getId())
                .petName(b.getPet().getName())
                .customerId(b.getUser().getId())
                .customerName(b.getUser().getFullName())
                .serviceId(b.getService().getId())
                .serviceName(b.getService().getServiceName())
                .staffId(b.getStaff() != null ? b.getStaff().getId() : null)
                .staffName(b.getStaff() != null ? b.getStaff().getFullName() : null)
                .appointmentDatetime(b.getAppointmentDatetime())
                .status(b.getStatus())
                .note(b.getNote())
                .createdAt(b.getCreatedAt())
                .build();
    }

    // ─── Staff: get my tasks ──────────────────────────────────────────────────

    @Override
    public List<TaskResponse> getMyTasks(String staffEmail) {
        Staff staff = resolveStaffByEmail(staffEmail);
        return bookingRepository.findByStaffId(staff.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Staff/Owner: get unassigned tasks ────────────────────────────────────

    @Override
    public List<TaskResponse> getUnassignedTasks(String requesterEmail) {
        // Resolve either staff or owner
        User user = resolveUser(requesterEmail);

        boolean isOwner = user.getRoles().stream()
                .anyMatch(r -> "SHOP_OWNER".equals(r.getName()));

        Shop shop;
        if (isOwner) {
            shop = resolveOwnerShop(requesterEmail);
        } else {
            Staff staff = staffRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
            shop = staff.getShop();
        }

        List<Booking> unassigned = bookingRepository.findByShopIdAndStaffIsNull(shop.getId());

        // If requester is staff and shop is in MANUAL or AUTO mode, hide unassigned tasks
        if (!isOwner && !"OPEN_POOL".equals(shop.getAssignmentMode())) {
            return List.of();
        }

        return unassigned.stream()
                .filter(b -> !List.of("CANCELLED", "COMPLETED").contains(b.getStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Staff: claim a task (OPEN_POOL) ──────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse claimTask(int bookingId, String staffEmail) {
        Staff staff = resolveStaffByEmail(staffEmail);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Booking must belong to the staff's shop
        if (booking.getShop().getId() != staff.getShop().getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);

        // Shop MUST be in OPEN_POOL mode for staff to claim
        if (!"OPEN_POOL".equals(staff.getShop().getAssignmentMode())) {
            throw new AppException(ErrorCode.MANUAL_ASSIGNMENT_ONLY);
        }

        // Must be unassigned
        if (booking.getStaff() != null)
            throw new AppException(ErrorCode.BOOKING_ALREADY_ASSIGNED);

        booking.setStaff(staff);
        bookingRepository.save(booking);
        log.info("Staff {} claimed booking {}", staff.getFullName(), bookingId);
        return toResponse(booking);
    }

    // ─── Owner: assign task to a specific staff ────────────────────────────────

    @Override
    @Transactional
    public TaskResponse assignTask(int bookingId, int staffId, String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staff.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);

        booking.setStaff(staff);
        bookingRepository.save(booking);
        log.info("Owner assigned staff {} to booking {}", staff.getFullName(), bookingId);
        return toResponse(booking);
    }

    // ─── Owner: unassign staff from a booking ────────────────────────────────

    @Override
    @Transactional
    public TaskResponse unassignTask(int bookingId, String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);

        booking.setStaff(null);
        bookingRepository.save(booking);
        log.info("Owner unassigned staff from booking {}", bookingId);
        return toResponse(booking);
    }

    // ─── Staff: update task status ─────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(int bookingId, TaskStatusUpdateRequest request, String requesterEmail) {
        User requester = resolveUser(requesterEmail);
        boolean isOwner = requester.getRoles().stream().anyMatch(r -> "SHOP_OWNER".equals(r.getName()));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (isOwner) {
            Shop shop = resolveOwnerShop(requesterEmail);
            if (booking.getShop().getId() != shop.getId())
                throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        } else {
            Staff staff = staffRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
            // Booking must be assigned to THIS staff
            if (booking.getStaff() == null || booking.getStaff().getId() != staff.getId())
                throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        }

        String newStatus = request.getStatus().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus))
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);

        // Enforce forward-only transitions
        String current = booking.getStatus();
        if (!isValidTransition(current, newStatus))
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);

        booking.setStatus(newStatus);
        bookingRepository.save(booking);
        log.info("Requester {} updated booking {} status: {} → {}", requesterEmail, bookingId, current, newStatus);

        // Cập nhật ví khi booking hoàn thành
        if ("COMPLETED".equals(newStatus)) {
            walletService.onBookingCompleted(bookingId);
        }
        // CANCELLED → không cần làm gì với ví (tiền chưa bao giờ vào ví)

        return toResponse(booking);
    }

    // ─── Owner: get all shop tasks ─────────────────────────────────────────────

    @Override
    public List<TaskResponse> getAllShopTasks(String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);
        return bookingRepository.findByShopId(shop.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Status transition validation ─────────────────────────────────────────

    private boolean isValidTransition(String current, String next) {
        return switch (current) {
            case "PENDING_PAYMENT" -> false; // Still waiting for payment
            case "CONFIRMED"       -> Set.of("IN_PROGRESS", "CANCELLED").contains(next);
            case "IN_PROGRESS"     -> Set.of("COMPLETED", "CANCELLED").contains(next);
            case "COMPLETED", "CANCELLED" -> false; // Terminal states
            default                -> false;
        };
    }
}
