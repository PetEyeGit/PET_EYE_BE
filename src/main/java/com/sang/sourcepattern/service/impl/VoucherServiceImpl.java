package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.entity.UserVoucher;
import com.sang.sourcepattern.entity.Voucher;
import com.sang.sourcepattern.repository.UserVoucherRepository;
import com.sang.sourcepattern.repository.VoucherRepository;
import com.sang.sourcepattern.service.VoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    VoucherRepository voucherRepository;
    UserVoucherRepository userVoucherRepository;

    @Override
    public boolean issueNewcomerVouchers(User user) {
        List<Voucher> newcomerVouchers = voucherRepository.findByVoucherTypeAndActiveTrue("NEWCOMER");


        if (newcomerVouchers.isEmpty()) {
            log.debug("Khong co voucher NEWCOMER nao duoc cau hinh, bo qua.");
            return false;
        }

        boolean issuedAny = false;

        for (Voucher voucher : newcomerVouchers) {
            // Idempotent: user da nhan voucher nay chua?
            if (userVoucherRepository.existsByUserIdAndVoucherId(user.getId(), voucher.getId())) {
                log.debug("User {} da nhan voucher {} truoc do, bo qua.", user.getEmail(), voucher.getCode());
                continue;
            }

            // Kiem tra quota
            long issued = userVoucherRepository.countByVoucherId(voucher.getId());
            if (voucher.getIssueQuantity() != null && issued >= voucher.getIssueQuantity()) {
                log.info("Voucher {} da het quota ({}/{}), khong phat them.",
                        voucher.getCode(), issued, voucher.getIssueQuantity());
                continue;
            }

            // Tinh han su dung
            int validDays = (voucher.getValidDays() != null) ? voucher.getValidDays() : 30;
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(validDays);

            UserVoucher userVoucher = UserVoucher.builder()
                    .user(user)
                    .voucher(voucher)
                    .isUsed(false)
                    .expiresAt(expiresAt)
                    .build();
            userVoucherRepository.save(userVoucher);
            issuedAny = true;

            log.info("Da phat voucher TAN THU '{}' (giam {}%) cho user '{}'. Quota: {}/{}",
                    voucher.getCode(),
                    voucher.getDiscountValue(),
                    user.getEmail(),
                    issued + 1,
                    voucher.getIssueQuantity());
        }
        
        return issuedAny;
    }
}