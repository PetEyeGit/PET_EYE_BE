package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.response.PageResponse;
import com.sang.sourcepattern.dto.response.TransactionResponse;

import java.time.LocalDateTime;

public interface TransactionService {
    PageResponse<TransactionResponse> getCustomerTransactions(String email, int page, int size);
    PageResponse<TransactionResponse> getShopTransactions(String email, int page, int size);
    PageResponse<TransactionResponse> getAllTransactionsForAdmin(Integer shopId, String status, String type, String source, String search, int page, int size);
    TransactionResponse updateTransactionDate(int id, LocalDateTime createdAt);
}

