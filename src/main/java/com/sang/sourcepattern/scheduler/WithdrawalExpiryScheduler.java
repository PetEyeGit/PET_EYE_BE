package com.sang.sourcepattern.scheduler;

import com.sang.sourcepattern.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job: mỗi giờ kiểm tra và expire các WithdrawalRequest
 * ở trạng thái PAYING quá 24h mà admin chưa xác nhận.
 *
 * Khi expire:
 *  - Status → EXPIRED
 *  - Tiền hoàn về ShopWallet.availableBalance
 *  - Shop có thể tạo yêu cầu rút mới
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawalExpiryScheduler {

    private final WalletService walletService;

    /**
     * Chạy mỗi giờ (cron: giây phút giờ ngày tháng thứ).
     * fixedDelay = 3_600_000ms = 1 giờ, initialDelay = 60s (chờ app khởi động xong).
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void expireStalePayouts() {
        log.info("[Scheduler] Running expireStalePayouts...");
        try {
            walletService.expireStalePayouts();
        } catch (Exception e) {
            log.error("[Scheduler] expireStalePayouts failed: {}", e.getMessage(), e);
        }
    }
}
