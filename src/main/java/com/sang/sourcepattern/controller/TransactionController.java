package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.response.ApiResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Transaction", description = "Transaction management for customers")
public class TransactionController {

    TransactionService transactionService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lấy lịch sử giao dịch của khách hàng")
    public ApiResponse<List<TransactionResponse>> getMyTransactions(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<List<TransactionResponse>>builder()
                .result(transactionService.getMyTransactions(jwt.getClaim("email")))
                .build();
    }
}
