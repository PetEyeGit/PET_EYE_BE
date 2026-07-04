package com.sang.sourcepattern.scheduler;

import com.sang.sourcepattern.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job: dọn dẹp các tài khoản chưa xác thực email (OTP) quá 10 phút.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnverifiedUserCleanupScheduler {

    private final UserService userService;

    /**
     * Chạy mỗi 5 phút (300,000ms), bắt đầu chạy sau khi app khởi động 10 giây (10,000ms).
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 10_000)
    public void cleanExpiredUnverifiedUsers() {
        log.info("[Scheduler] Running cleanExpiredUnverifiedUsers...");
        try {
            userService.deleteExpiredUnverifiedUsers();
        } catch (Exception e) {
            log.error("[Scheduler] cleanExpiredUnverifiedUsers failed: {}", e.getMessage(), e);
        }
    }
}
