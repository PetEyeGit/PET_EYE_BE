package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.BookingCreationRequest;
import com.sang.sourcepattern.dto.request.CancelBookingRequest;
import com.sang.sourcepattern.dto.request.InitiatePaymentRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.InitiatePaymentResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Booking", description = "Book services and handle PayOS payment")
public class BookingController {

    BookingService bookingService;

    // ─── PayOS flow ───────────────────────────────────────────────────────────

    @PostMapping("/initiate-payment")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Step 1: Validate inputs + create PayOS link. No booking saved yet.")
    public ApiResponse<InitiatePaymentResponse> initiatePayment(
            @RequestBody @Valid InitiatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<InitiatePaymentResponse>builder()
                .result(bookingService.initiatePayment(request, jwt.getClaim("email")))
                .message("Redirect user to checkoutUrl to complete payment.")
                .build();
    }

    @PostMapping("/confirm-payment")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Step 2: Verify PayOS payment → create booking if PAID.")
    public ApiResponse<BookingResponse> confirmPayment(
            @RequestParam long orderCode,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.confirmPayment(orderCode, jwt.getClaim("email")))
                .build();
    }

    @PostMapping("/mock-confirm")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mock: Bypass PayOS and confirm payment directly")
    public ApiResponse<BookingResponse> mockConfirmPayment(
            @RequestParam long orderCode,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.mockConfirmPayment(orderCode, jwt.getClaim("email")))
                .build();
    }

    // ─── Cash flow (2-step: 10% deposit via PayOS + 90% cash at venue) ─────────

    @PostMapping("/cash/initiate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cash Step 1: Validate inputs + create PayOS link for 10% deposit (= admin commission). No booking saved yet.")
    public ApiResponse<InitiatePaymentResponse> initiateCashDeposit(
            @RequestBody @Valid BookingCreationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<InitiatePaymentResponse>builder()
                .result(bookingService.initiateCashDeposit(request, jwt.getClaim("email")))
                .message("Please pay the 10% deposit via the checkoutUrl. Remaining 90% is paid in cash at the venue.")
                .build();
    }

    @PostMapping("/cash/confirm")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cash Step 2: Verify 10% deposit payment → create booking if PAID.")
    public ApiResponse<BookingResponse> confirmCashDeposit(
            @RequestParam long orderCode,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.confirmCashDeposit(orderCode, jwt.getClaim("email")))
                .message("Booking confirmed. Please pay the remaining 90% in cash at the venue.")
                .build();
    }

    @PostMapping("/cash/mock-confirm")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mock: Bypass PayOS and confirm cash deposit directly")
    public ApiResponse<BookingResponse> mockConfirmCashDeposit(
            @RequestParam long orderCode,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.mockConfirmCashDeposit(orderCode, jwt.getClaim("email")))
                .message("Booking confirmed (MOCK). Please pay the remaining 90% in cash at the venue.")
                .build();
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all bookings of the authenticated user")
    public ApiResponse<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<BookingResponse>>builder()
                .result(bookingService.getMyBookings(jwt.getClaim("email")))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'SHOP_OWNER', 'STAFF', 'ADMIN')")
    @Operation(summary = "Get booking detail by id")
    public ApiResponse<BookingResponse> getBookingById(
            @PathVariable int id,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.getBookingById(id, jwt.getClaim("email")))
                .build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'SHOP_OWNER')")
    @Operation(summary = "Cancel a booking")
    public ApiResponse<BookingResponse> cancelBooking(
            @PathVariable int id,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.cancelBooking(id, jwt.getClaim("email")))
                .message("Booking cancelled")
                .build();
    }

    @PostMapping("/{id}/cancel-request")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Request cancellation of a booking pending shop approval")
    public ApiResponse<BookingResponse> requestBookingCancellation(
            @PathVariable int id,
            @RequestBody @Valid CancelBookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.requestBookingCancellation(
                        id,
                        request.getReason(),
                        request.getBankName(),
                        request.getBankAccount(),
                        request.getAccountHolder(),
                        jwt.getClaim("email")
                ))
                .message("Booking cancellation request submitted")
                .build();
    }

    @PutMapping("/{id}/bank-info")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update bank info for refund when status is WAITING_REFUND")
    public ApiResponse<BookingResponse> updateBankInfo(
            @PathVariable int id,
            @RequestBody @Valid com.sang.sourcepattern.dto.request.UpdateBankInfoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.updateBankInfo(
                        id,
                        request.getBankName(),
                        request.getBankAccount(),
                        request.getAccountHolder(),
                        jwt.getClaim("email")
                ))
                .message("Bank info updated successfully")
                .build();
    }

    // ─── Public helpers ───────────────────────────────────────────────────────

    @GetMapping("/staff/{shopId}")
    @Operation(summary = "Get active staff list for a shop (public)")
    public ApiResponse<List<StaffResponse>> getShopStaff(@PathVariable int shopId) {
        return ApiResponse.<List<StaffResponse>>builder()
                .result(bookingService.getShopStaff(shopId))
                .build();
    }

    @GetMapping("/staff/{shopId}/availability")
    @Operation(summary = "Get staff list with availability for a specific time slot")
    public ApiResponse<List<StaffResponse>> getShopStaffAvailability(
            @PathVariable int shopId,
            @RequestParam(required = false) String appointmentDatetime,
            @RequestParam(defaultValue = "60") int durationMinutes) {

        LocalDateTime dt = null;
        if (appointmentDatetime != null && !appointmentDatetime.isBlank()) {
            try {
                dt = LocalDateTime.parse(appointmentDatetime);
            } catch (Exception e) {
                try {
                    dt = LocalDateTime.parse(appointmentDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception ex) {
                    // If parsing fails, proceed without datetime filter (return all staff)
                }
            }
        }

        return ApiResponse.<List<StaffResponse>>builder()
                .result(bookingService.getShopStaffWithAvailability(shopId, dt, durationMinutes))
                .build();
    }

    @GetMapping("/pet/{petId}/availability")
    @Operation(summary = "Check if pet is available for booking at a specific time")
    public ApiResponse<Boolean> checkPetAvailability(
            @PathVariable int petId,
            @RequestParam String appointmentDatetime,
            @RequestParam(defaultValue = "60") int durationMinutes) {

        LocalDateTime dt;
        try {
            dt = LocalDateTime.parse(appointmentDatetime);
        } catch (Exception e) {
            dt = LocalDateTime.parse(appointmentDatetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        return ApiResponse.<Boolean>builder()
                .result(bookingService.checkPetAvailability(petId, dt, durationMinutes))
                .build();
    }

    @GetMapping("/shop/{shopId}/available-slots")
    @Operation(summary = "Get available time slots for a shop on a specific date. "
            + "Only slots where at least one staff member is free are returned.")
    public ApiResponse<List<LocalDateTime>> getAvailableTimeSlots(
            @PathVariable int shopId,
            @RequestParam String date,
            @RequestParam(defaultValue = "60") int durationMinutes) {

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return ApiResponse.<List<LocalDateTime>>builder()
                .result(bookingService.getAvailableTimeSlots(shopId, localDate, durationMinutes))
                .build();
    }

    @GetMapping("/shop/{shopId}/available-slots-by-services")
    @Operation(summary = "Get available time slots for a shop given a list of service IDs. "
            + "Total duration = sum of all selected services. Slots step = 60 min. "
            + "A slot is available if at least one staff is free for the full total duration.")
    public ApiResponse<List<LocalDateTime>> getAvailableTimeSlotsForServices(
            @PathVariable int shopId,
            @RequestParam String date,
            @RequestParam List<Integer> serviceIds) {

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return ApiResponse.<List<LocalDateTime>>builder()
                .result(bookingService.getAvailableTimeSlotsForServices(shopId, localDate, serviceIds))
                .build();
    }

    @GetMapping("/shop")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'STAFF')")
    @Operation(summary = "Get all bookings for the authenticated shop owner or staff within a range")
    public ApiResponse<List<BookingResponse>> getShopBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse.<List<BookingResponse>>builder()
                .result(bookingService.getShopBookings(jwt.getClaim("email"), start, end))
                .build();
    }
}
