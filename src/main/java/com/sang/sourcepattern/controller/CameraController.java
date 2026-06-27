package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.repository.StaffRepository;
import com.sang.sourcepattern.service.CameraService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/camera")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CameraController {

    CameraService cameraService;
    BookingRepository bookingRepository;
    ShopRepository shopRepository;
    UserRepository userRepository;
    StaffRepository staffRepository;

    @Value("${camera.kbone.stream-url}")
    @NonFinal
    String streamUrl;

    @GetMapping("/stream/kbone")
    public Map<String, String> getStreamUrl() {
        Map<String, String> response = new HashMap<>();
        response.put("streamUrl", streamUrl);
        return response;
    }

    /**
     * Get active camera streams for the authenticated customer.
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<List<BookingResponse>> getActiveCameras(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaim("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<BookingResponse> activeCameras = bookingRepository.findByUserId(user.getId()).stream()
                .filter(b -> List.of("CONFIRMED", "IN_PROGRESS").contains(b.getStatus()))
                .filter(b -> b.getServices() != null && b.getServices().stream().anyMatch(s -> s.getCategory() != null &&
                        (s.getCategory().equalsIgnoreCase("BOARDING") ||
                                s.getCategory().equalsIgnoreCase("Hotel"))))
                .filter(b -> b.getServices() != null && b.getServices().stream().anyMatch(com.sang.sourcepattern.entity.Service::isCameraEnabled))
                .filter(b -> b.getCameraStreamUrl() != null && !b.getCameraStreamUrl().isBlank())
                .map(this::toCustomerResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<BookingResponse>>builder()
                .result(activeCameras)
                .build();
    }

    /**
     * Configure RTSP url for a booking (Shop Owner or Staff).
     */
    @PostMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'STAFF')")
    public ApiResponse<BookingResponse> configureCamera(
            @PathVariable int bookingId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaim("email");
        int shopId;
        
        var shopOpt = shopRepository.findByOwnerEmail(email);
        if (shopOpt.isPresent()) {
            shopId = shopOpt.get().getId();
        } else {
            var staffOpt = staffRepository.findByUserEmail(email);
            if (staffOpt.isPresent()) {
                shopId = staffOpt.get().getShop().getId();
            } else {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getShop().getId() != shopId) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        }

        boolean isLodging = booking.getServices() != null && booking.getServices().stream().anyMatch(s -> s.getCategory() != null &&
                (s.getCategory().equalsIgnoreCase("BOARDING") ||
                        s.getCategory().equalsIgnoreCase("Hotel")));
        boolean cameraEnabled = isLodging && booking.getServices() != null &&
                booking.getServices().stream().anyMatch(com.sang.sourcepattern.entity.Service::isCameraEnabled);

        if (!cameraEnabled) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (!List.of("CONFIRMED", "IN_PROGRESS", "COMPLETED").contains(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_STATUS_INVALID);
        }

        String rtspUrl = body.get("rtspUrl");
        if (rtspUrl == null || rtspUrl.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Start background docker container and save stream url
        String streamHlsUrl = cameraService.startStream(bookingId, rtspUrl.trim());
        booking.setCameraRtspUrl(rtspUrl.trim());
        booking.setCameraStreamUrl(streamHlsUrl);
        booking.setCameraConfiguredAt(LocalDateTime.now());
        bookingRepository.save(booking);

        log.info("User {} configured camera RTSP for booking {}: stream URL = {}", email, bookingId, streamHlsUrl);

        return ApiResponse.<BookingResponse>builder()
                .result(toOwnerResponse(booking))
                .build();
    }

    /**
     * Stop and delete camera RTSP streaming for a booking (Shop Owner or Staff).
     */
    @DeleteMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'STAFF')")
    public ApiResponse<BookingResponse> deleteCamera(
            @PathVariable int bookingId,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaim("email");
        int shopId;
        
        var shopOpt = shopRepository.findByOwnerEmail(email);
        if (shopOpt.isPresent()) {
            shopId = shopOpt.get().getId();
        } else {
            var staffOpt = staffRepository.findByUserEmail(email);
            if (staffOpt.isPresent()) {
                shopId = staffOpt.get().getShop().getId();
            } else {
                throw new AppException(ErrorCode.SHOP_NOT_FOUND);
            }
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getShop().getId() != shopId) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_STAFF_SHOP);
        }

        cameraService.stopStream(bookingId);
        booking.setCameraRtspUrl(null);
        booking.setCameraStreamUrl(null);
        booking.setCameraConfiguredAt(null);
        bookingRepository.save(booking);

        log.info("User {} deleted camera configuration for booking {}", email, bookingId);

        return ApiResponse.<BookingResponse>builder()
                .result(toOwnerResponse(booking))
                .build();
    }

    private BookingResponse toCustomerResponse(Booking booking) {
        com.sang.sourcepattern.entity.Service primaryService = (booking.getServices() != null && !booking.getServices().isEmpty())
                ? booking.getServices().iterator().next() : null;

        java.math.BigDecimal totalServicePrice = (booking.getServices() != null)
                ? booking.getServices().stream()
                        .map(s -> s.getPrice() != null ? s.getPrice() : java.math.BigDecimal.ZERO)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                : java.math.BigDecimal.ZERO;

        int totalDuration = (booking.getServices() != null)
                ? booking.getServices().stream().mapToInt(s -> s.getDurationMinutes() != 0 ? s.getDurationMinutes() : 60).sum()
                : 60;

        java.util.List<BookingResponse.BookingServiceDto> serviceDtos = (booking.getServices() != null)
                ? booking.getServices().stream().map(s -> BookingResponse.BookingServiceDto.builder()
                        .serviceId(s.getId())
                        .serviceName(s.getServiceName())
                        .servicePrice(s.getPrice())
                        .build()).collect(Collectors.toList())
                : new java.util.ArrayList<>();

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
                .cameraRtspUrl(null) // Hide RTSP url for customers
                .cameraStreamUrl(booking.getCameraStreamUrl())
                .cameraEnabled(true)
                .cameraConfiguredAt(booking.getCameraConfiguredAt())
                .serviceEndDatetime(booking.getAppointmentDatetime() != null
                        ? booking.getAppointmentDatetime().plusMinutes(totalDuration)
                        : null)
                .build();
    }

    private BookingResponse toOwnerResponse(Booking booking) {
        com.sang.sourcepattern.entity.Service primaryService = (booking.getServices() != null && !booking.getServices().isEmpty())
                ? booking.getServices().iterator().next() : null;

        java.math.BigDecimal totalServicePrice = (booking.getServices() != null)
                ? booking.getServices().stream()
                        .map(s -> s.getPrice() != null ? s.getPrice() : java.math.BigDecimal.ZERO)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                : java.math.BigDecimal.ZERO;

        int totalDuration = (booking.getServices() != null)
                ? booking.getServices().stream().mapToInt(s -> s.getDurationMinutes() != 0 ? s.getDurationMinutes() : 60).sum()
                : 60;

        java.util.List<BookingResponse.BookingServiceDto> serviceDtos = (booking.getServices() != null)
                ? booking.getServices().stream().map(s -> BookingResponse.BookingServiceDto.builder()
                        .serviceId(s.getId())
                        .serviceName(s.getServiceName())
                        .servicePrice(s.getPrice())
                        .build()).collect(Collectors.toList())
                : new java.util.ArrayList<>();

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
                .cameraRtspUrl(booking.getCameraRtspUrl()) // Shop owner / staff can see RTSP URL
                .cameraStreamUrl(booking.getCameraStreamUrl())
                .cameraEnabled(true)
                .cameraConfiguredAt(booking.getCameraConfiguredAt())
                .serviceEndDatetime(booking.getAppointmentDatetime() != null
                        ? booking.getAppointmentDatetime().plusMinutes(totalDuration)
                        : null)
                .build();
    }
}
