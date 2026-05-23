package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.WithdrawalRequestCreate;
import com.sang.sourcepattern.dto.response.ShopWalletResponse;
import com.sang.sourcepattern.dto.response.WithdrawalRequestResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    /** Lấy ví của shop hiện tại (SHOP_OWNER) */
    ShopWalletResponse getMyWallet(String ownerEmail);

    /** Lấy ví của 1 shop theo ID (ADMIN) */
    ShopWalletResponse getWalletByShopId(int shopId);

    /** Lấy tổng số dư ví admin (tổng tiền phí đã thu) */
    BigDecimal getAdminBalance();

    /** Lấy tỷ lệ phí hoa hồng admin (mặc định 10%) */
    BigDecimal getAdminFeeRate();

    /**
     * Khi booking COMPLETED: chuyển tiền từ frozen → available (90%) và ghi nhận phí admin (10%).
     * Gọi từ TaskServiceImpl khi status → COMPLETED.
     */
    void onBookingCompleted(int bookingId);

    /**
     * Khi booking CANCELLED: hoàn lại tiền frozen về 0 (không cộng available).
     * Gọi từ TaskServiceImpl khi status → CANCELLED.
     */
    void onBookingCancelled(int bookingId);

    /** Shop tạo yêu cầu rút tiền */
    WithdrawalRequestResponse createWithdrawalRequest(WithdrawalRequestCreate request, String ownerEmail);

    /** Lấy danh sách yêu cầu rút tiền của shop hiện tại */
    List<WithdrawalRequestResponse> getMyWithdrawalRequests(String ownerEmail);

    /** Admin: lấy tất cả yêu cầu rút tiền */
    List<WithdrawalRequestResponse> getAllWithdrawalRequests();

    /** Admin: lấy yêu cầu rút tiền theo trạng thái */
    List<WithdrawalRequestResponse> getWithdrawalRequestsByStatus(String status);

    /** Admin duyệt yêu cầu rút tiền */
    WithdrawalRequestResponse approveWithdrawal(int requestId, String adminNote);

    /**
     * Admin khởi tạo PayOS payout link để chuyển tiền cho shop.
     * Trả về checkoutUrl để admin thực hiện thanh toán.
     * Status chuyển sang PAYING.
     */
    WithdrawalRequestResponse initiatePayoutPayOS(int requestId, String adminNote);

    /**
     * Tạo lại PayOS link mới cho withdrawal đang ở trạng thái PAYING
     * (trường hợp admin đã vào PayOS nhưng ấn huỷ, link cũ hết hạn).
     * Tạo orderCode mới, cập nhật checkoutUrl, giữ nguyên status PAYING.
     */
    WithdrawalRequestResponse regeneratePayoutLink(int requestId);

    /**
     * Xác nhận PayOS payout thành công (gọi sau khi PayOS callback PAID).
     * Status chuyển sang APPROVED, trừ totalWithdrawn.
     */
    WithdrawalRequestResponse confirmPayout(long orderCode);

    /**
     * Admin xác nhận thủ công đã chuyển khoản cho shop (không qua PayOS).
     * Status chuyển sang APPROVED, trừ totalWithdrawn.
     */
    WithdrawalRequestResponse confirmManualWithdrawal(int requestId);

    /** Admin xác nhận hoàn tiền cho đơn đã huỷ (Admin quét QR và chuyển tiền cho khách).
     *  Trừ `availableBalance` và `totalEarned` của shop, ghi Transaction type=REFUND.
     */
    void confirmRefundForBooking(int bookingId);

    /** Admin từ chối yêu cầu rút tiền */
    WithdrawalRequestResponse rejectWithdrawal(int requestId, String adminNote);

    /**
     * Tự động expire các withdrawal PAYING quá 24h:
     * - Đổi status → EXPIRED
     * - Hoàn tiền về availableBalance của shop
     * Gọi bởi scheduled job mỗi giờ.
     */
    void expireStalePayouts();
}
