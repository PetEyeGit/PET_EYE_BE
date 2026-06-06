package com.sang.sourcepattern.service;

import com.sang.sourcepattern.entity.Booking;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String fullName, String token);
    void sendPasswordResetEmail(String toEmail, String fullName, String token);
    void sendShopApprovedEmail(String toEmail, String shopName);
    void sendShopRejectedEmail(String toEmail, String shopName, String reason);

    /**
     * Gửi hóa đơn thanh toán cho khách hàng qua email.
     *
     * @param toEmail       email khách hàng
     * @param booking       booking đã hoàn tất
     * @param paidAmount    số tiền đã thanh toán (VND)
     * @param paymentMethod phương thức thanh toán (PAYOS / CASH_DEPOSIT / CASH)
     * @param paymentStatus trạng thái thanh toán (ví dụ: "Đã thanh toán thành công")
     */
    void sendBookingInvoiceEmail(String toEmail, Booking booking,
                                 java.math.BigDecimal paidAmount,
                                 String paymentMethod,
                                 String paymentStatus);
}
