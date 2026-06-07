package com.sang.sourcepattern.scheduler;

import com.sang.sourcepattern.entity.Booking;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AppointmentReminderScheduler {

    BookingRepository bookingRepository;
    EmailService emailService;

    /**
     * Chạy định kỳ mỗi 30 phút (1800000 ms)
     * Tìm các booking CONFIRMED diễn ra trong vòng 24h tới và chưa gửi email nhắc.
     */
    @Scheduled(fixedRate = 1800000)
    public void sendReminders() {
        log.info("Bắt đầu quét và gửi email nhắc lịch hẹn trước 24 giờ...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime within24Hours = now.plusHours(24);

        // Lấy tất cả booking được CONFIRMED và có lịch hẹn từ hiện tại đến 24 giờ tới, chưa gửi nhắc nhở
        List<Booking> upcomingBookings = bookingRepository.findBookingsForReminder(
                "CONFIRMED",
                now,
                within24Hours
        );

        if (upcomingBookings.isEmpty()) {
            log.info("Không có lịch hẹn nào cần nhắc nhở trong khoảng thời gian này.");
            return;
        }

        int count = 0;
        for (Booking booking : upcomingBookings) {
            try {
                if (booking.getUser() != null && booking.getUser().getEmail() != null) {
                    emailService.sendAppointmentReminderEmail(booking.getUser().getEmail(), booking);
                    
                    // Cập nhật trạng thái đã gửi để không bị lặp
                    booking.setIsReminderSent(true);
                    bookingRepository.save(booking);
                    count++;
                }
            } catch (Exception e) {
                log.error("Lỗi khi gửi email nhắc nhở cho Booking ID {}: {}", booking.getId(), e.getMessage());
            }
        }

        log.info("Đã hoàn tất gửi email nhắc lịch hẹn. Tổng số email gửi: {}", count);
    }
}
