package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.entity.*;
import com.sang.sourcepattern.repository.*;
import com.sang.sourcepattern.service.TierUpgradeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TierUpgradeServiceImpl implements TierUpgradeService {

    UserRepository userRepository;
    BookingRepository bookingRepository;
    MembershipTierRepository membershipTierRepository;
    VoucherRepository voucherRepository;
    UserVoucherRepository userVoucherRepository;
    com.sang.sourcepattern.service.SystemConfigService systemConfigService;

    @Override
    @Transactional
    public void processTierUpgrade(User user) {
        if (!systemConfigService.isVoucherServiceEnabled()) {
            log.info("Tier upgrade skipped for user {} because voucher/tier service is globally disabled.", user.getEmail());
            return;
        }

        // 1. Calculate total spending from COMPLETED bookings
        List<Booking> completedBookings = bookingRepository.findByUserId(user.getId())
                .stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .toList();

        double totalSpending = completedBookings.stream()
                .map(b -> {
                    if (b.getServices() == null || b.getServices().isEmpty()) {
                        return BigDecimal.ZERO;
                    }
                    return b.getServices().stream()
                            .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        user.setTotalSpending(totalSpending);

        // 2. Find the highest tier the user qualifies for
        List<MembershipTier> allTiers = membershipTierRepository.findAll();
        MembershipTier newTier = null;
        double maxQualifiedSpending = -1;

        for (MembershipTier tier : allTiers) {
            if (totalSpending >= tier.getRequiredSpending() && tier.getRequiredSpending() >= maxQualifiedSpending) {
                newTier = tier;
                maxQualifiedSpending = tier.getRequiredSpending();
            }
        }

        // 3. If tier changed, update and issue vouchers
        if (newTier != null) {
            boolean isUpgraded = user.getCurrentTier() == null || user.getCurrentTier().getId() != newTier.getId();
            
            if (isUpgraded) {
                user.setCurrentTier(newTier);
                user.setJustUpgraded(true);
                log.info("User {} upgraded to tier {}", user.getEmail(), newTier.getName());

                // Issue vouchers for the new tier
                // Chi phat voucher TIER dang ACTIVE (chua bi tat)
                List<Voucher> tierVouchers = voucherRepository.findByTargetTierNameAndActiveTrue(newTier.getName());

                for (Voucher voucher : tierVouchers) {
                    for (int i = 0; i < voucher.getIssueQuantity(); i++) {
                        UserVoucher userVoucher = UserVoucher.builder()
                                .user(user)
                                .voucher(voucher)
                                .isUsed(false)
                                .expiresAt(java.time.LocalDateTime.now().plusDays(voucher.getValidDays() != null ? voucher.getValidDays() : 30))
                                .build();
                        userVoucherRepository.save(userVoucher);
                    }
                    log.info("Issued {}x voucher {} to user {}", voucher.getIssueQuantity(), voucher.getCode(), user.getEmail());
                }
            }
        }

        userRepository.save(user);
    }
}
