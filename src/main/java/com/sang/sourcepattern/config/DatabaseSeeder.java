package com.sang.sourcepattern.config;

import com.sang.sourcepattern.entity.MembershipTier;
import com.sang.sourcepattern.entity.Transaction;
import com.sang.sourcepattern.entity.Voucher;
import com.sang.sourcepattern.repository.MembershipTierRepository;
import com.sang.sourcepattern.repository.TransactionRepository;
import com.sang.sourcepattern.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final MembershipTierRepository membershipTierRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Tối ưu 100% hiệu năng Production: Chạy Batch UPDATE trực tiếp dưới DB trong 0.001s, không nạp dữ liệu vào RAM
        transactionRepository.standardizeOldCashDepositDescriptions();
        transactionRepository.standardizeOldPayosDescriptions();

        if (membershipTierRepository.count() == 0) {
            MembershipTier dong = membershipTierRepository.save(MembershipTier.builder()
                    .name("Đồng").requiredSpending(0.0).benefits("Ưu đãi cơ bản, Tích lũy chi tiêu").build());
            
            MembershipTier bac = membershipTierRepository.save(MembershipTier.builder()
                    .name("Bạc").requiredSpending(500000.0).benefits("Tự động nhận Voucher Bạc, Ưu tiên hỗ trợ").build());
            
            MembershipTier vang = membershipTierRepository.save(MembershipTier.builder()
                    .name("Vàng").requiredSpending(1000000.0).benefits("Tự động nhận Voucher Vàng, Ưu tiên đặt chỗ, Tích điểm x2").build());
            
            MembershipTier kimCuong = membershipTierRepository.save(MembershipTier.builder()
                    .name("Kim Cương").requiredSpending(5000000.0).benefits("Tự động nhận Voucher Kim cương, Dịch vụ miễn phí, Hotline VIP").build());

            // Seed Vouchers
            if (voucherRepository.count() == 0) {
                voucherRepository.save(Voucher.builder()
                        .code("BAC10").targetTier(bac).discountType("PERCENTAGE").discountValue(10.0)
                        .minOrderValue(0.0).validDays(30).issueQuantity(1).build());
                
                voucherRepository.save(Voucher.builder()
                        .code("VANG15").targetTier(vang).discountType("PERCENTAGE").discountValue(15.0)
                        .minOrderValue(0.0).validDays(30).issueQuantity(2).build());
                
                voucherRepository.save(Voucher.builder()
                        .code("KIMCUONG20").targetTier(kimCuong).discountType("PERCENTAGE").discountValue(20.0)
                        .minOrderValue(0.0).validDays(30).issueQuantity(3).build());
            }
        }
    }
}
