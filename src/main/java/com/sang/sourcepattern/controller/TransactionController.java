package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.PageResponse;
import com.sang.sourcepattern.dto.response.TransactionResponse;
import com.sang.sourcepattern.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.sang.sourcepattern.dto.request.UpdateTransactionDateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Transaction", description = "Transaction management for customers")
public class TransactionController {

    TransactionService transactionService;

    @GetMapping("/customer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lấy lịch sử giao dịch của khách hàng")
    public ApiResponse<PageResponse<TransactionResponse>> getCustomerTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.<PageResponse<TransactionResponse>>builder()
                .result(transactionService.getCustomerTransactions(jwt.getClaim("email"), page, size))
                .build();
    }

    @GetMapping("/shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Lấy lịch sử biến động số dư của shop")
    public ApiResponse<PageResponse<TransactionResponse>> getShopTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<TransactionResponse>>builder()
                .result(transactionService.getShopTransactions(jwt.getClaim("email"), page, size))
                .build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin lấy danh sách tất cả giao dịch toàn hệ thống có bộ lọc")
    public ApiResponse<PageResponse<TransactionResponse>> getAllTransactionsForAdmin(
            @RequestParam(required = false) Integer shopId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<TransactionResponse>>builder()
                .result(transactionService.getAllTransactionsForAdmin(shopId, status, type, source, search, page, size))
                .build();
    }

    @PutMapping("/admin/{id}/created-at")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin chỉnh sửa ngày tạo giao dịch (Fake / rải đều lịch sử)")
    public ApiResponse<TransactionResponse> updateTransactionDate(
            @PathVariable int id,
            @RequestBody @Valid UpdateTransactionDateRequest request) {
        return ApiResponse.<TransactionResponse>builder()
                .result(transactionService.updateTransactionDate(id, request.getCreatedAt()))
                .build();
    }
}

