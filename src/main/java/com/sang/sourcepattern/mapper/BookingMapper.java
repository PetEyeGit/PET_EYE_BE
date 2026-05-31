package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    public BookingResponse toBookingResponse(Booking booking) {
        if (booking == null) return null;
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
                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhone() : null)
                .staffId(booking.getStaff() != null ? booking.getStaff().getId() : null)
                .staffName(booking.getStaff() != null ? booking.getStaff().getFullName() : null)
                .appointmentDatetime(booking.getAppointmentDatetime())
                .status(booking.getStatus())
                .note(booking.getNote())
                .cancellationReason(booking.getCancellationReason())
                .payosOrderCode(booking.getPayosOrderCode())
                .createdAt(booking.getCreatedAt())
                .serviceStartDatetime(booking.getServiceStartDatetime())
                .serviceEndDatetime(booking.getServiceEndDatetime())
                .cameraRtspUrl(booking.getCameraRtspUrl())
                .cameraStreamUrl(booking.getCameraStreamUrl())
                .cameraEnabled(booking.getService() != null && booking.getService().isCameraEnabled())
                .cameraConfiguredAt(booking.getCameraConfiguredAt())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .serviceEndDatetime(booking.getService() != null && "BOARDING".equalsIgnoreCase(booking.getService().getCategory()) && booking.getCheckOut() != null
                        ? booking.getCheckOut()
                        : (booking.getAppointmentDatetime() != null && booking.getService() != null
                                ? booking.getAppointmentDatetime().plusMinutes(booking.getService().getDurationMinutes())
                                : null))
                .cageSize(booking.getCageSize())
                .roomType(booking.getRoomType())
                .build();
    }
}
