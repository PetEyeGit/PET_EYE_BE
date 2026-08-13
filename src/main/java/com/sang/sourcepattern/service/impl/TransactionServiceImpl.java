package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.response.TransactionResponse;
import com.sang.sourcepattern.entity.Transaction;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.TransactionRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.service.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.sang.sourcepattern.dto.response.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionServiceImpl implements TransactionService {

    TransactionRepository transactionRepository;
    UserRepository userRepository;

    ShopRepository shopRepository;

    @Override
    public PageResponse<TransactionResponse> getCustomerTransactions(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Transaction> transactionPage = transactionRepository.findByBookingUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<TransactionResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .last(transactionPage.isLast())
                .build();
    }

    @Override
    public PageResponse<TransactionResponse> getShopTransactions(String email, int page, int size) {
        Shop shop = shopRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Transaction> transactionPage = transactionRepository.findByShopIdOrderByCreatedAtDesc(shop.getId(), pageable);

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<TransactionResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .last(transactionPage.isLast())
                .build();
    }

    @Override
    public PageResponse<TransactionResponse> getAllTransactionsForAdmin(Integer shopId, String status, String type, String source, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<Transaction> transactionPage = transactionRepository.searchTransactionsForAdmin(
                shopId,
                (status != null && !status.isBlank()) ? status : null,
                (type != null && !type.isBlank()) ? type : null,
                (source != null && !source.isBlank()) ? source : null,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<TransactionResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .last(transactionPage.isLast())
                .build();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public TransactionResponse updateTransactionDate(int id, java.time.LocalDateTime createdAt) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        transaction.setCreatedAt(createdAt);
        if (transaction.getCompletedAt() != null) {
            transaction.setCompletedAt(createdAt);
        }
        if (transaction.getBooking() != null) {
            transaction.getBooking().setCreatedAt(createdAt);
        }

        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    private TransactionResponse mapToResponse(Transaction t) {

        TransactionResponse.TransactionResponseBuilder builder = TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .paymentMethod(t.getPaymentMethod())
                .status(t.getStatus())
                .payosOrderCode(t.getPayosOrderCode())
                .gatewayTransactionId(t.getGatewayTransactionId())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt() != null ? t.getCompletedAt() : t.getCreatedAt());

        if (t.getShop() != null) {
            builder.shopId(t.getShop().getId());
            builder.shopName(t.getShop().getShopName());
        }

        if (t.getBooking() != null) {
            builder.bookingId(t.getBooking().getId());
            if (t.getBooking().getShop() != null) {
                builder.shopId(t.getBooking().getShop().getId());
                builder.shopName(t.getBooking().getShop().getShopName());
            }
            if (t.getBooking().getUser() != null) {
                builder.customerName(t.getBooking().getUser().getFullName());
                builder.customerEmail(t.getBooking().getUser().getEmail());
            }
            if (t.getBooking().getServices() != null && !t.getBooking().getServices().isEmpty()) {
                String serviceNames = t.getBooking().getServices().stream()
                        .map(com.sang.sourcepattern.entity.Service::getServiceName)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(", "));
                builder.serviceName(serviceNames);
            }
        }
        return builder.build();
    }
}
