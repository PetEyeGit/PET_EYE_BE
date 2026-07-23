package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    List<Voucher> findByTargetTierName(String targetTierName);

    /** Lấy tất cả voucher theo loại (TIER / NEWCOMER) */
    List<Voucher> findByVoucherType(String voucherType);

    /** Chi lay voucher dang ACTIVE theo loai */
    List<Voucher> findByVoucherTypeAndActiveTrue(String voucherType);

    /** Chi lay voucher TIER dang ACTIVE theo ten hang */
    List<Voucher> findByTargetTierNameAndActiveTrue(String targetTierName);

    /** Tìm voucher theo mã code và đang active */
    java.util.Optional<Voucher> findByCodeAndActiveTrue(String code);
}
