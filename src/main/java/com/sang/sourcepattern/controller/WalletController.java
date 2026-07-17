package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.WithdrawalRequestCreate;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ShopWalletResponse;
import com.sang.sourcepattern.dto.response.WithdrawalRequestResponse;
import com.sang.sourcepattern.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Wallet", description = "Shop wallet & withdrawal management")
public class WalletController {

    WalletService walletService;

    // ─── Shop Owner endpoints ─────────────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Lấy thông tin ví của shop hiện tại")
    public ApiResponse<ShopWalletResponse> getMyWallet(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<ShopWalletResponse>builder()
                .result(walletService.getMyWallet(jwt.getClaim("email")))
                .build();
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Tạo yêu cầu rút tiền")
    public ApiResponse<WithdrawalRequestResponse> createWithdrawal(
            @RequestBody @Valid WithdrawalRequestCreate request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<WithdrawalRequestResponse>builder()
                .result(walletService.createWithdrawalRequest(request, jwt.getClaim("email")))
                .message("Yêu cầu rút tiền đã được gửi, chờ Admin duyệt.")
                .build();
    }

    @GetMapping("/withdrawals/my")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Lấy lịch sử yêu cầu rút tiền của shop")
    public ApiResponse<List<WithdrawalRequestResponse>> getMyWithdrawals(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<WithdrawalRequestResponse>>builder()
                .result(walletService.getMyWithdrawalRequests(jwt.getClaim("email")))
                .build();
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping("/admin/balance")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tổng số dư ví Admin (tổng phí 10% đã thu)")
    public ApiResponse<Map<String, BigDecimal>> getAdminBalance() {
        return ApiResponse.<Map<String, BigDecimal>>builder()
                .result(Map.of("adminBalance", walletService.getAdminBalance(), "totalFrozenBalance", walletService.getTotalFrozenBalance()))
                .build();
    }

    @GetMapping("/admin/shop/{shopId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin xem ví của 1 shop cụ thể")
    public ApiResponse<ShopWalletResponse> getShopWallet(@PathVariable int shopId) {
        return ApiResponse.<ShopWalletResponse>builder()
                .result(walletService.getWalletByShopId(shopId))
                .build();
    }

    @GetMapping("/admin/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin lấy tất cả yêu cầu rút tiền")
    public ApiResponse<List<WithdrawalRequestResponse>> getAllWithdrawals(
            @RequestParam(required = false) String status) {
        List<WithdrawalRequestResponse> result = (status != null && !status.isBlank())
                ? walletService.getWithdrawalRequestsByStatus(status)
                : walletService.getAllWithdrawalRequests();
        return ApiResponse.<List<WithdrawalRequestResponse>>builder()
                .result(result)
                .build();
    }

    @PostMapping("/admin/withdrawals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin duyệt: tạo PayOS link để chuyển tiền cho shop")
    public ApiResponse<WithdrawalRequestResponse> approveWithdrawal(
            @PathVariable int id,
            @RequestParam(required = false, defaultValue = "") String adminNote) {
        return ApiResponse.<WithdrawalRequestResponse>builder()
                .result(walletService.initiatePayoutPayOS(id, adminNote))
                .message("Đã tạo link thanh toán PayOS. Vui lòng hoàn tất chuyển khoản.")
                .build();
    }

    @PostMapping("/admin/withdrawals/confirm-payout")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xác nhận PayOS payout thành công → APPROVED + trừ totalWithdrawn")
    public ApiResponse<WithdrawalRequestResponse> confirmPayout(
            @RequestParam long orderCode) {
        return ApiResponse.<WithdrawalRequestResponse>builder()
                .result(walletService.confirmPayout(orderCode))
                .message("Đã xác nhận chuyển tiền thành công.")
                .build();
    }

    @PostMapping("/admin/withdrawals/{id}/confirm-manual")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin xác nhận thủ công đã chuyển khoản cho shop (không qua PayOS)")
    public ApiResponse<WithdrawalRequestResponse> confirmWithdrawal(
            @PathVariable int id) {
        return ApiResponse.<WithdrawalRequestResponse>builder()
                .result(walletService.confirmManualWithdrawal(id))
                .message("Đã xác nhận chuyển khoản thủ công thành công.")
                .build();
    }

        @PostMapping("/admin/refunds/{bookingId}/confirm")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Admin xác nhận hoàn tiền cho đơn đã huỷ (quét QR)")
        public ApiResponse<String> confirmRefundForBooking(@PathVariable("bookingId") int bookingId) {
                walletService.confirmRefundForBooking(bookingId);
                return ApiResponse.<String>builder()
                                .result("OK")
                                .message("Đã hoàn tiền cho khách hàng và cập nhật ví shop.")
                                .build();
        }

    @GetMapping("/admin/refunds/waiting")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin lấy danh sách yêu cầu hoàn tiền cho khách hàng")
    public ApiResponse<List<WithdrawalRequestResponse>> getWaitingRefunds() {
        return ApiResponse.<List<WithdrawalRequestResponse>>builder()
                .result(walletService.getWaitingRefundsAsWithdrawals())
                .build();
    }

    @PostMapping("/admin/withdrawals/{id}/regenerate-payout")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo lại PayOS link mới khi link cũ đã bị huỷ/hết hạn (status PAYING)")
    public ApiResponse<WithdrawalRequestResponse> regeneratePayoutLink(@PathVariable int id) {
        return ApiResponse.<WithdrawalRequestResponse>builder()
                .result(walletService.regeneratePayoutLink(id))
                .message("Đã tạo lại link PayOS mới. Vui lòng hoàn tất chuyển khoản.")
                .build();
    }

    @PostMapping("/admin/withdrawals/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin từ chối yêu cầu rút tiền")
    public ApiResponse<WithdrawalRequestResponse> rejectWithdrawal(
            @PathVariable int id,
            @RequestParam(required = false, defaultValue = "") String adminNote) {
        return ApiResponse.<WithdrawalRequestResponse>builder()
                .result(walletService.rejectWithdrawal(id, adminNote))
                .message("Đã từ chối yêu cầu rút tiền.")
                .build();
    }

    @PostMapping("/admin/withdrawals/expire-stale")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thủ công trigger expire các PAYING quá 24h (dùng để test)")
    public ApiResponse<String> expireStale() {
        walletService.expireStalePayouts();
        return ApiResponse.<String>builder()
                .result("Đã kiểm tra và expire các withdrawal PAYING quá hạn.")
                .build();
    }
}
