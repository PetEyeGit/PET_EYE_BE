package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.BookingCreationRequest;
import com.sang.sourcepattern.dto.request.InitiatePaymentRequest;
import com.sang.sourcepattern.dto.request.ShopCreateBookingRequest;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.InitiatePaymentResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;

import com.sang.sourcepattern.dto.response.PageResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    /**
     * STEP 1 — PayOS flow:
     * Validate inputs, create PayOS payment link, return checkoutUrl.
     * NO booking is saved to DB yet.
     */
    InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request, String userEmail);

    /**
     * STEP 2 — PayOS flow:
     * Called after PayOS redirects back with ?orderCode=xxx.
     * Queries PayOS API; if PAID → creates Booking + Payment in DB → CONFIRMED.
     * If CANCELLED/EXPIRED → returns error, nothing saved.
     */
    BookingResponse confirmPayment(long orderCode, String userEmail);

    /**
     * MOCK flow:
     * Bypass PayOS verification and confirm the booking immediately.
     */
    BookingResponse mockConfirmPayment(long orderCode, String userEmail);

    /**
     * MOCK CASH flow:
     * Bypass PayOS verification and confirm the cash deposit directly.
     */
    BookingResponse mockConfirmCashDeposit(long orderCode, String userEmail);

    /**
     * CASH flow — STEP 1:
     * Validate inputs, create PayOS payment link for 10% deposit (= admin commission).
     * NO booking is saved to DB yet.
     */
    InitiatePaymentResponse initiateCashDeposit(BookingCreationRequest request, String userEmail);

    /**
     * CASH flow — STEP 2:
     * Called after user pays the 10% deposit via PayOS.
     * Queries PayOS API; if PAID → creates Booking + Payment in DB → CONFIRMED.
     * Remaining 90% is collected in cash at the venue.
     */
    BookingResponse confirmCashDeposit(long orderCode, String userEmail);

    /** Get all bookings of the authenticated user (không phân trang — backward compat) */
    List<BookingResponse> getMyBookings(String userEmail);

    /** Get bookings of the authenticated user — paginated, filter by status group */
    com.sang.sourcepattern.dto.response.PageResponse<BookingResponse> getMyBookings(
            String userEmail, int page, int size, String status);

    /** Get a single booking detail */
    BookingResponse getBookingById(int bookingId, String userEmail);

    /** Update booking details */
    Object updateBooking(int bookingId, com.sang.sourcepattern.dto.request.UpdateBookingRequest request, String userEmail);

    /** Cancel a booking */
    BookingResponse cancelBooking(int bookingId, String userEmail);

    /** Shop proactively cancels a booking */
    BookingResponse shopCancelBooking(int bookingId, String reason, String requesterEmail);

    BookingResponse updateBankInfo(int bookingId, String bankName, String bankAccount, String accountHolder, String userEmail);

    /** Request cancellation and send to shop approval */
    BookingResponse requestBookingCancellation(int bookingId, String reason, String bankName, String bankAccount, String accountHolder, String userEmail);

    /** Get active staff list for a shop */
    List<StaffResponse> getShopStaff(int shopId);

    /**
     * Get active staff list for a shop with availability check.
     * If appointmentDatetime + durationMinutes provided, marks each staff as available/busy.
     */
    List<StaffResponse> getShopStaffWithAvailability(int shopId, LocalDateTime appointmentDatetime, int durationMinutes);

    /** Get all bookings for a shop owner within a range */
    List<BookingResponse> getShopBookings(String ownerEmail, LocalDateTime start, LocalDateTime end);
    com.sang.sourcepattern.dto.response.PageResponse<BookingResponse> getShopBookingsPaged(
            String ownerEmail, LocalDateTime start, LocalDateTime end, int page);

    /** Check if pet is available for booking at a specific time */
    boolean checkPetAvailability(int petId, LocalDateTime appointmentDatetime, int durationMinutes);

    /**
     * Get available time slots for a shop on a specific date.
     * A slot is "available" if at least one active staff member is free during that slot.
     * Slots are generated from shop's openTime to closeTime with the given step (durationMinutes).
     *
     * @param shopId          shop to check
     * @param date            the date to check (yyyy-MM-dd)
     * @param durationMinutes duration of the service (used for conflict window + slot step)
     * @return list of LocalDateTime representing available slot start times
     */
    List<LocalDateTime> getAvailableTimeSlots(int shopId, LocalDate date, int durationMinutes);

    /**
     * Get available time slots for a shop on a specific date, given a list of service IDs.
     *
     * Logic:
     * - Tổng duration = sum(durationMinutes) của tất cả services được chọn.
     * - Slots được sinh theo bước 60 phút (cố định) từ openTime đến closeTime.
     * - Một slot available khi có ít nhất 1 staff rảnh trong khoảng
     *   [slotStart, slotStart + totalDuration].
     *
     * @param shopId     shop to check
     * @param date       the date to check (yyyy-MM-dd)
     * @param serviceIds list of service IDs the user wants to book
     * @return list of LocalDateTime representing available slot start times
     */
    List<LocalDateTime> getAvailableTimeSlotsForServices(int shopId, LocalDate date, List<Integer> serviceIds);

    /**
     * Shop xuất hóa đơn thủ công và gửi về email khách hàng.
     * Chỉ shop owner / staff của shop đó mới được gọi.
     *
     * @param bookingId    booking cần xuất hóa đơn
     * @param requesterEmail email của shop owner hoặc staff thực hiện
     */
    void sendInvoice(int bookingId, String requesterEmail);

    /**
     * Shop tạo lịch hẹn trực tiếp cho khách.
     */
    BookingResponse createBookingByShop(ShopCreateBookingRequest request, String requesterEmail);
}
