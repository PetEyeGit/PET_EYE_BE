package com.sang.sourcepattern.mapper;

import com.sang.sourcepattern.dto.response.BookingResponse;
import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.entity.Service;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {
    public BookingResponse toBookingResponse(Booking booking) {
        if (booking == null) return null;

        Service primaryService = (booking.getServices() != null && !booking.getServices().isEmpty())
                ? booking.getServices().iterator().next() : null;

        java.math.BigDecimal totalServicePrice = (booking.getServices() != null)
                ? booking.getServices().stream()
                        .map(s -> s.getPrice() != null ? s.getPrice() : java.math.BigDecimal.ZERO)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                : java.math.BigDecimal.ZERO;

        boolean isBoarding = booking.getServices() != null && booking.getServices().stream()
                .anyMatch(s -> s.getCategory() != null
                        && ("BOARDING".equalsIgnoreCase(s.getCategory()) || "Hotel".equalsIgnoreCase(s.getCategory())));

        boolean cameraEnabled = isBoarding && booking.getServices() != null
                && booking.getServices().stream().anyMatch(Service::isCameraEnabled);

        int totalDuration = (booking.getServices() != null)
                ? booking.getServices().stream().mapToInt(s -> s.getDurationMinutes() != 0 ? s.getDurationMinutes() : 60).sum()
                : 60;

        java.util.List<BookingResponse.BookingServiceDto> serviceDtos = (booking.getServices() != null)
                ? booking.getServices().stream().map(s -> BookingResponse.BookingServiceDto.builder()
                        .serviceId(s.getId())
                        .serviceName(s.getServiceName())
                        .servicePrice(s.getPrice())
                        .build()).collect(java.util.stream.Collectors.toList())
                : new java.util.ArrayList<>();

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .shopId(booking.getShop().getId())
                .shopName(booking.getShop().getShopName())
                .serviceId(primaryService != null ? primaryService.getId() : 0)
                .serviceName(primaryService != null ? primaryService.getServiceName() : "")
                .servicePrice(totalServicePrice)
                .services(serviceDtos)
                .petId(booking.getPet().getId())
                .petName(booking.getPet().getName())
                .customerName(booking.getUser() != null ? booking.getUser().getFullName() : null)
                .customerEmail(booking.getUser() != null ? booking.getUser().getEmail() : null)
                .customerPhone(booking.getUser() != null ? booking.getUser().getPhone() : null)
                .customerAddress(booking.getUser() != null ? booking.getUser().getAddress() : null)
                .staffId(booking.getStaff() != null ? booking.getStaff().getId() : null)
                .staffName(booking.getStaff() != null ? booking.getStaff().getFullName() : null)
                .appointmentDatetime(booking.getAppointmentDatetime())
                .status(booking.getStatus())
                .note(booking.getNote())
                .cancellationReason(booking.getCancellationReason())
                .payosOrderCode(booking.getPayosOrderCode())
                .createdAt(booking.getCreatedAt())
                .serviceStartDatetime(booking.getServiceStartDatetime())
                .serviceEndDatetime(isBoarding && booking.getCheckOut() != null
                        ? booking.getCheckOut()
                        : (booking.getAppointmentDatetime() != null
                                ? booking.getAppointmentDatetime().plusMinutes(totalDuration)
                                : null))
                .cameraRtspUrl(booking.getCameraRtspUrl())
                .cameraStreamUrl(booking.getCameraStreamUrl())
                .cameraEnabled(cameraEnabled)
                .cameraConfiguredAt(booking.getCameraConfiguredAt())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .petWeight(booking.getPetWeight())
                .roomType(booking.getRoomType())
                .build();
    }
}
