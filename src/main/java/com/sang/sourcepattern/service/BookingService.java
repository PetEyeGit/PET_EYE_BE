package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.BookingCreationRequest;
import com.sang.sourcepattern.dto.request.InitiatePaymentRequest;
import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.dto.response.InitiatePaymentResponse;
import com.sang.sourcepattern.dto.response.StaffResponse;

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
     * CASH flow: create booking immediately, status = CONFIRMED.
     */
    BookingResponse createCashBooking(BookingCreationRequest request, String userEmail);

    /** Get all bookings of the authenticated user */
    List<BookingResponse> getMyBookings(String userEmail);

    /** Get a single booking detail */
    BookingResponse getBookingById(int bookingId, String userEmail);

    /** Cancel a booking */
    BookingResponse cancelBooking(int bookingId, String userEmail);

    /** Get active staff list for a shop */
    List<StaffResponse> getShopStaff(int shopId);

    /** Get all bookings for a shop owner within a range */
    List<BookingResponse> getShopBookings(String ownerEmail, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
