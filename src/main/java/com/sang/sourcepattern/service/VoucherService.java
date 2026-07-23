package com.sang.sourcepattern.service;

import com.sang.sourcepattern.entity.User;

public interface VoucherService {

    /**
     * Phat tat ca voucher NEWCOMER (danh cho First Booking) con quota cho user.
     * Idempotent: goi nhieu lan cung khong cap trung cho cung mot user.
     *
     * @param user user vua hoan tat booking
     */
    boolean issueFirstBookingVouchers(User user);

    /**
     * Nguoi dung nhap ma voucher de luu vao vi
     */
    com.sang.sourcepattern.entity.UserVoucher claimVoucherByCode(User user, String code);
}