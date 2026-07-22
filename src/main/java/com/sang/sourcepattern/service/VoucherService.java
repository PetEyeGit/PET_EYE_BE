package com.sang.sourcepattern.service;

import com.sang.sourcepattern.entity.User;

public interface VoucherService {

    /**
     * Phat tat ca voucher NEWCOMER con quota cho user moi dang ky.
     * Idempotent: goi nhieu lan cung khong cap trung cho cung mot user.
     *
     * @param user user vua hoan tat dang ky (email verified hoac social login lan dau)
     */
    boolean issueNewcomerVouchers(User user);
}