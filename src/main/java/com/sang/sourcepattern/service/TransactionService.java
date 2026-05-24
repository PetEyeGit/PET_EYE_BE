package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.response.TransactionResponse;
import java.util.List;

public interface TransactionService {
    /**
     * Lấy danh sách giao dịch của user đang đăng nhập (khách hàng)
     */
    List<TransactionResponse> getMyTransactions(String email);
}
