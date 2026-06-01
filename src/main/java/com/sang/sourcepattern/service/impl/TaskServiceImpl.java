package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.TaskStatusUpdateRequest;
import com.sang.sourcepattern.dto.response.TaskResponse;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.Staff;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.entity.Notification;
import com.sang.sourcepattern.entity.StaffChangeRequest;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.NotificationRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.StaffChangeRequestRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.repository.PetMedicalRecordRepository;
import com.sang.sourcepattern.repository.PaymentRepository;
import com.sang.sourcepattern.repository.TransactionRepository;
import com.sang.sourcepattern.service.TierUpgradeService;
import com.sang.sourcepattern.service.TaskService;
import com.sang.sourcepattern.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    TierUpgradeService tierUpgradeService;
    StaffChangeRequestRepository staffChangeRequestRepository;
    NotificationRepository notificationRepository;
    com.sang.sourcepattern.service.CameraService cameraService;
    PetMedicalRecordRepository petMedicalRecordRepository;
    PaymentRepository paymentRepository;
    TransactionRepository transactionRepository;

    // Valid status transition chain for Staff
    private static final Set<String> VALID_STATUSES = Set.of("CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "CANCEL_REQUESTED");

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
        // Primary service = first in Set
        com.sang.sourcepattern.entity.Service primarySvc =
                (b.getServices() != null && !b.getServices().isEmpty()) ? b.getServices().iterator().next() : null;
        // Total price = sum all services
        java.math.BigDecimal totalPrice = (b.getServices() != null)
                ? b.getServices().stream()
                    .map(s -> s.getPrice() != null ? s.getPrice() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                : java.math.BigDecimal.ZERO;
        // Category: boarding if any service is boarding
        boolean isBoarding = b.getServices() != null && b.getServices().stream()
                .anyMatch(s -> s.getCategory() != null
                        && (s.getCategory().equalsIgnoreCase("BOARDING") || s.getCategory().equalsIgnoreCase("Hotel")));
        boolean isClinic = b.getServices() != null && b.getServices().stream()
                .anyMatch(s -> "CLINIC".equalsIgnoreCase(s.getCategory()));
        String category = isBoarding ? "BOARDING" : (isClinic ? "CLINIC" : (primarySvc != null ? primarySvc.getCategory() : null));
        boolean cameraEnabled = b.getServices() != null && b.getServices().stream()
                .anyMatch(com.sang.sourcepattern.entity.Service::isCameraEnabled);
        // Services list for task
        java.util.List<TaskResponse.ServiceItem> serviceItems = (b.getServices() != null)
                ? b.getServices().stream().map(s -> TaskResponse.ServiceItem.builder()
                        .serviceId(s.getId()).serviceName(s.getServiceName()).servicePrice(s.getPrice()).build())
                        .collect(java.util.stream.Collectors.toList())
                : java.util.Collections.emptyList();

        return TaskResponse.builder()
                .bookingId(b.getId())
                .shopId(b.getShop().getId())
                .shopName(b.getShop().getShopName())
                .petId(b.getPet().getId())
                .petName(b.getPet().getName())
                .customerId(b.getUser().getId())
                .customerName(b.getUser().getFullName())
                .customerEmail(b.getUser().getEmail())
                .customerPhone(b.getUser().getPhone())
                .serviceId(primarySvc != null ? primarySvc.getId() : 0)
                .serviceName(primarySvc != null ? primarySvc.getServiceName() : "")
                .servicePrice(totalPrice)
                .services(serviceItems)
                .staffId(b.getStaff() != null ? b.getStaff().getId() : null)
                .staffName(b.getStaff() != null ? b.getStaff().getFullName() : null)
                .appointmentDatetime(b.getAppointmentDatetime())
                .checkOutDatetime(b.getCheckOutDatetime())
                .checkOut(b.getCheckOut())
                .serviceStartDatetime(b.getServiceStartDatetime())
                .serviceEndDatetime(b.getServiceEndDatetime())
                .cageSize(b.getCageSize())
                .roomType(b.getRoomType())
                .status(b.getStatus())
                .cameraEnabled(cameraEnabled)
                .rtspLink(b.getRtspLink())
                .category(category)
                .note(b.getNote())
                .cancellationReason(b.getCancellationReason())
                .bankName(b.getBankName())
                .bankAccount(b.getBankAccount())
                .accountHolder(b.getAccountHolder())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    // ─── Staff: get my tasks ──────────────────────────────────────────────────

    @Override
    public List<TaskResponse> getMyTasks(String staffEmail) {
        Staff staff = resolveStaffByEmail(staffEmail);
        return bookingRepository.findByStaffIdWithServices(staff.getId())
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

        List<Booking> unassigned = bookingRepository.findByShopIdAndStaffIsNullWithServices(shop.getId());

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
        booking.setStatus("WAITING_SHOP_APPROVAL");
        bookingRepository.save(booking);

        // --- Notification cho Shop Owner ---
        Notification notifOwner = Notification.builder()
                .user(booking.getShop().getOwner())
                .title("Nhân viên đã nhận lịch")
                .content(String.format("Nhân viên %s vừa nhận thực hiện đơn #%d.", staff.getFullName(), bookingId))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifOwner);
        // ------------------------------------

        log.info("Staff {} claimed booking {} (waiting for shop approval)", staff.getFullName(), bookingId);
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

        if (List.of("IN_PROGRESS", "COMPLETED", "CANCELLED").contains(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staff.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);

        // Check if there is already a pending request for this booking
        List<StaffChangeRequest> pendingRequests = staffChangeRequestRepository.findByBookingIdAndStatus(bookingId, "PENDING");
        if (!pendingRequests.isEmpty()) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_STATUS_WHILE_REQUEST_PENDING);
        }

        // Enforce request-flow if staff is already assigned, regardless of whether booking is approved or not
        if (List.of("WAITING_SHOP_APPROVAL", "CONFIRMED", "IN_PROGRESS").contains(booking.getStatus()) 
                && booking.getStaff() != null && booking.getStaff().getId() != staffId) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_STAFF_DIRECTLY);
        }

        booking.setStaff(staff);
        bookingRepository.save(booking);

        // --- Notification cho Staff mới ---
        if (staff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(staff.getUser())
                    .title("Bạn được phân công lịch hẹn mới")
                    .content(String.format("Bạn vừa được chủ shop phân công thực hiện lịch hẹn #%d.", bookingId))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }
        // -----------------------------------

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

        if (List.of("COMPLETED", "CANCELLED").contains(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        // Check if there is already a pending request for this booking
        List<StaffChangeRequest> pendingRequests = staffChangeRequestRepository.findByBookingIdAndStatus(bookingId, "PENDING");
        if (!pendingRequests.isEmpty()) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_STATUS_WHILE_REQUEST_PENDING);
        }

        // Prevent direct unassignment if the booking has a staff assigned
        if (List.of("WAITING_SHOP_APPROVAL", "CONFIRMED", "IN_PROGRESS").contains(booking.getStatus()) 
                && booking.getStaff() != null) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_STAFF_DIRECTLY);
        }

        Staff oldStaff = booking.getStaff();
        booking.setStaff(null);
        bookingRepository.save(booking);

        // --- Notification cho Staff cũ ---
        if (oldStaff != null && oldStaff.getUser() != null) {
            Notification notifStaff = Notification.builder()
                    .user(oldStaff.getUser())
                    .title("Đã gỡ lịch hẹn")
                    .content(String.format("Bạn không còn phụ trách lịch hẹn #%d nữa.", bookingId))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifStaff);
        }
        // -----------------------------------

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

        // Prevent status updates if a staff change request is pending
        List<StaffChangeRequest> pendingRequests = staffChangeRequestRepository.findByBookingIdAndStatus(bookingId, "PENDING");
        if (!pendingRequests.isEmpty()) {
            throw new AppException(ErrorCode.CANNOT_UPDATE_STATUS_WHILE_REQUEST_PENDING);
        }

        String newStatus = request.getStatus().toUpperCase();
        if (!VALID_STATUSES.contains(newStatus))
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);

        // Enforce forward-only transitions
        String current = booking.getStatus();
        if (!isValidTransition(current, newStatus))
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
            
        // Check medical record constraint for CLINIC services when completing
        if ("COMPLETED".equals(newStatus)) {
            boolean isClinicBooking = booking.getServices() != null && booking.getServices().stream()
                    .anyMatch(s -> "CLINIC".equalsIgnoreCase(s.getCategory()));
            if (isClinicBooking) {
                int count = petMedicalRecordRepository.countByBookingId(bookingId);
                if (count == 0) {
                    throw new AppException(ErrorCode.MISSING_MEDICAL_RECORD);
                }
            }
        }

        booking.setStatus(newStatus);
        if ("IN_PROGRESS".equals(newStatus) && booking.getServiceStartDatetime() == null) {
            booking.setServiceStartDatetime(LocalDateTime.now());
        }
        if ("COMPLETED".equals(newStatus)) {
            booking.setServiceEndDatetime(LocalDateTime.now());
        }
        if (request.getRtspLink() != null && !request.getRtspLink().isBlank()) {
            booking.setRtspLink(request.getRtspLink());
        }
        bookingRepository.save(booking);
        log.info("Requester {} updated booking {} status: {} → {}", requesterEmail, bookingId, current, newStatus);

        // --- Notifications cho User ---
        if (!current.equals(newStatus)) {
            String title = null;
            String content = null;
            switch (newStatus) {
                case "CONFIRMED":
                    title = "Shop đã xác nhận lịch hẹn";
                    content = String.format("Shop đã xác nhận lịch hẹn #%d của bạn.", bookingId);
                    break;
                case "IN_PROGRESS":
                    title = "Đang thực hiện dịch vụ";
                    content = String.format("Thú cưng của bạn đang được phục vụ (đơn #%d).", bookingId);
                    break;
                case "COMPLETED":
                    title = "Dịch vụ đã hoàn thành";
                    content = String.format("Dịch vụ đã hoàn thành cho đơn #%d. Cảm ơn bạn!", bookingId);
                    break;
                case "CANCELLED":
                    title = "Lịch hẹn đã bị hủy";
                    content = String.format("Lịch hẹn #%d đã bị hủy bởi shop.", bookingId);
                    break;
            }
            if (title != null && content != null) {
                Notification notifUser = Notification.builder()
                        .user(booking.getUser())
                        .title(title)
                        .content(content)
                        .notificationType(Notification.NotificationType.BOOKING)
                        .build();
                notificationRepository.save(notifUser);
            }
        }
        // ------------------------------

        if ("COMPLETED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            cameraService.stopStream(bookingId);
        }

        // Cập nhật ví khi booking hoàn thành
        if ("COMPLETED".equals(newStatus)) {
            walletService.onBookingCompleted(bookingId);
            tierUpgradeService.processTierUpgrade(booking.getUser());
        }
        // CANCELLED → không cần làm gì với ví (tiền chưa bao giờ vào ví)

        return toResponse(booking);
    }

    // ─── Owner: get all shop tasks ─────────────────────────────────────────────

    @Override
    public List<TaskResponse> getAllShopTasks(String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);
        return bookingRepository.findByShopIdWithServices(shop.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<StaffChangeRequest> getPendingStaffChangeRequest(int bookingId) {
        return staffChangeRequestRepository.findByBookingIdAndStatus(bookingId, "PENDING");
    }

    // ─── Owner: request to change staff for a booking ──────────────────────────

    @Override
    @Transactional
    public void requestStaffChange(int bookingId, int proposedStaffId, String reason, String ownerEmail) {
        Shop shop = resolveOwnerShop(ownerEmail);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
                
        if (List.of("IN_PROGRESS", "COMPLETED", "CANCELLED").contains(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }
                
        if (booking.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
            
        Staff proposedStaff = staffRepository.findById(proposedStaffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
                
        if (proposedStaff.getShop().getId() != shop.getId())
            throw new AppException(ErrorCode.STAFF_NOT_BELONG_TO_SHOP);

        // Check if there is already a pending request for this booking
        List<StaffChangeRequest> pendingRequests = staffChangeRequestRepository.findByBookingIdAndStatus(bookingId, "PENDING");
        if (!pendingRequests.isEmpty()) {
            throw new AppException(ErrorCode.STAFF_CHANGE_REQUEST_ALREADY_EXISTS);
        }

        StaffChangeRequest request = StaffChangeRequest.builder()
                .booking(booking)
                .oldStaff(booking.getStaff())
                .proposedStaff(proposedStaff)
                .reason(reason)
                .status("PENDING")
                .build();
                
        staffChangeRequestRepository.save(request);

        // Notify user
        Notification notification = Notification.builder()
                .user(booking.getUser())
                .title("Yêu cầu đổi nhân viên")
                .content(String.format("Shop %s muốn đổi nhân viên cho lịch hẹn #%d của bạn. Lý do: %s", shop.getShopName(), bookingId, reason))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
                
        notificationRepository.save(notification);
        
        log.info("Owner requested staff change for booking {} to staff {}", bookingId, proposedStaff.getFullName());
    }

    // ─── Customer: respond to a staff change request ────────────────────────────

    @Override
    @Transactional
    public TaskResponse respondToStaffChange(int requestId, String status, String userEmail) {
        User user = resolveUser(userEmail);
        
        StaffChangeRequest request = staffChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));
                
        if (request.getBooking().getUser().getId() != user.getId())
            throw new AppException(ErrorCode.UNAUTHORIZED);

        if (!"PENDING".equals(request.getStatus())) {
            throw new AppException(ErrorCode.REQUEST_ALREADY_PROCESSED);
        }

        request.setStatus(status.toUpperCase());
        request.setProcessedAt(java.time.LocalDateTime.now());

        if ("ACCEPTED".equals(status.toUpperCase())) {
            Booking booking = request.getBooking();
            booking.setStaff(request.getProposedStaff());
            if ("WAITING_SHOP_APPROVAL".equals(booking.getStatus())) {
                booking.setStatus("CONFIRMED");
                log.info("Booking {} status automatically set to CONFIRMED after staff change acceptance", booking.getId());
            }
            bookingRepository.save(booking);
            log.info("User accepted staff change for booking {}", booking.getId());

            // Notify shop owner
            Notification notification = Notification.builder()
                    .user(booking.getShop().getOwner())
                    .title("Khách hàng đã đồng ý đổi nhân viên")
                    .content(String.format("Khách hàng %s đã đồng ý đổi nhân viên cho lịch hẹn #%d.", booking.getUser().getFullName(), booking.getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notification);
        } else if ("REJECTED".equals(status.toUpperCase())) {
            log.info("User rejected staff change for booking {}", request.getBooking().getId());

            // Notify shop owner
            Notification notification = Notification.builder()
                    .user(request.getBooking().getShop().getOwner())
                    .title("Khách hàng từ chối đổi nhân viên")
                    .content(String.format("Khách hàng %s đã từ chối đổi nhân viên cho lịch hẹn #%d.", request.getBooking().getUser().getFullName(), request.getBooking().getId()))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notification);
        } else {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        staffChangeRequestRepository.save(request);
        return toResponse(request.getBooking());
    }

    @Override
    public List<StaffChangeRequest> getStaffChangeHistory(int bookingId) {
        return staffChangeRequestRepository.findByBookingId(bookingId);
    }

    // ─── No-Show cancellation ─────────────────────────────────────────────────

    @Override
    @Transactional
    public TaskResponse cancelNoShow(int bookingId, String requesterEmail) {
        User requester = resolveUser(requesterEmail);
        boolean isOwner = requester.getRoles().stream().anyMatch(r -> "SHOP_OWNER".equals(r.getName()));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // ── Validate Role ────────────────────────────────────────────────────
        Shop shop;
        Staff staff = null;
        if (isOwner) {
            shop = resolveOwnerShop(requesterEmail);
            if (booking.getShop().getId() != shop.getId())
                throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        } else {
            staff = staffRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
            if (booking.getStaff() == null || booking.getStaff().getId() != staff.getId())
                throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
            shop = staff.getShop();
        }

        // ── Validate Status: chỉ cho hủy khi đơn ở WAITING_SHOP_APPROVAL hoặc CONFIRMED ──
        if (!List.of("WAITING_SHOP_APPROVAL", "CONFIRMED").contains(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        // ── Validate Time: Grace Period ──────────────────────────────────────
        int gracePeriod = shop.getLateGracePeriod(); // Lấy cấu hình từ Shop (default 15 phút)
        LocalDateTime deadline = booking.getAppointmentDatetime().plusMinutes(gracePeriod);
        if (LocalDateTime.now().isBefore(deadline)) {
            throw new AppException(ErrorCode.NO_SHOW_TOO_EARLY);
        }

        // ── Update Booking ───────────────────────────────────────────────────
        String previousStatus = booking.getStatus();
        booking.setStatus("CANCELLED");
        booking.setCancellationReason("LATE_NO_SHOW");
        bookingRepository.save(booking);

        // ── Update Payment → NO_SHOW_PENALTY (Admin giữ tiền cọc) ─────────
        paymentRepository.findByBookingId(bookingId).ifPresent(p -> {
            p.setStatus("NO_SHOW_PENALTY");
            paymentRepository.save(p);
        });

        // ── Update Transaction → NO_SHOW_PENALTY ─────────────────────────────
        transactionRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream().findFirst().ifPresent(t -> {
                    t.setStatus("NO_SHOW_PENALTY");
                    t.setNote("No-show by customer — deposit retained by Admin");
                    transactionRepository.save(t);
                });

        // ── Notifications ────────────────────────────────────────────────────
        // 1. Thông báo cho Khách hàng
        String staffName = (staff != null) ? staff.getFullName()
                : (booking.getStaff() != null ? booking.getStaff().getFullName() : "Shop");
        Notification notifUser = Notification.builder()
                .user(booking.getUser())
                .title("Lịch hẹn đã bị hủy do bạn không đến")
                .content(String.format(
                        "Lịch hẹn #%d của bạn đã bị hủy do bạn đến trễ quá %d phút so với giờ hẹn. "
                        + "Phí đặt cọc sẽ không được hoàn lại.",
                        bookingId, gracePeriod))
                .notificationType(Notification.NotificationType.BOOKING)
                .build();
        notificationRepository.save(notifUser);

        // 2. Thông báo cho Shop Owner (nếu Staff hủy)
        if (!isOwner && shop.getOwner() != null) {
            Notification notifOwner = Notification.builder()
                    .user(shop.getOwner())
                    .title("Nhân viên hủy đơn do khách không đến")
                    .content(String.format(
                            "Nhân viên %s vừa hủy đơn #%d do khách hàng %s không đến sau %d phút chờ.",
                            staffName, bookingId, booking.getUser().getFullName(), gracePeriod))
                    .notificationType(Notification.NotificationType.BOOKING)
                    .build();
            notificationRepository.save(notifOwner);
        }

        // Tắt camera stream nếu đang chạy
        cameraService.stopStream(bookingId);

        log.info("Booking {} cancelled as NO_SHOW by {} — grace period {}min elapsed (was {})",
                bookingId, requesterEmail, gracePeriod, previousStatus);

        return toResponse(booking);
    }

    // ─── Status transition validation ─────────────────────────────────────────

    private boolean isValidTransition(String current, String next) {
        if (current.equals(next)) return true; // Allow same status (e.g. for RTSP updates)
        return switch (current) {
            case "PENDING_PAYMENT" -> false; // Still waiting for payment
            case "WAITING_SHOP_APPROVAL" -> Set.of("CONFIRMED", "CANCELLED").contains(next);
            case "CONFIRMED"       -> Set.of("IN_PROGRESS", "CANCELLED").contains(next);
            case "IN_PROGRESS"     -> Set.of("COMPLETED", "CANCELLED").contains(next);
            case "CANCEL_REQUESTED" -> Set.of("CANCELLED").contains(next);
            case "COMPLETED", "CANCELLED" -> false; // Terminal states
            default                -> false;
        };
    }
}
