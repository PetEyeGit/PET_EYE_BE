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

    /**
     * Gửi email nhắc lịch hẹn trước 24 giờ kèm chính sách hủy.
     *
     * @param toEmail email khách hàng
     * @param booking thông tin lịch hẹn
     */
    void sendAppointmentReminderEmail(String toEmail, Booking booking);

    /**
     * Gửi email thông báo đơn hàng đã hoàn thành, chi tiết thời gian và cảm ơn.
     */
    void sendBookingCompletedEmail(String toEmail, Booking booking);

    /**
     * Gửi email thông báo cho khách hàng khi shop xin đổi nhân viên.
     */
    void sendStaffChangeRequestEmail(String toEmail, Booking booking, com.sang.sourcepattern.entity.Staff proposedStaff, String reason);

    /**
     * Gửi email thông báo có đơn đặt lịch mới cho Shop.
     */
    void sendNewBookingToShopEmail(String toEmail, Booking booking);

    /**
     * Gửi email thông báo cho Admin khi có cửa hàng đăng ký mới cần duyệt.
     */
    void sendNewShopRegistrationToAdminEmail(String adminEmail, com.sang.sourcepattern.entity.Shop shop);
}
