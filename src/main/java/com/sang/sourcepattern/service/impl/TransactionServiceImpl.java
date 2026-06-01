package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.response.TransactionResponse;
import com.sang.sourcepattern.entity.Transaction;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.TransactionRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.TransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionServiceImpl implements TransactionService {

    TransactionRepository transactionRepository;
    UserRepository userRepository;

    @Override
    public List<TransactionResponse> getMyTransactions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Transaction> transactions = transactionRepository.findByBookingUserIdOrderByCreatedAtDesc(user.getId());

        return transactions.stream().map(t -> {
            TransactionResponse.TransactionResponseBuilder builder = TransactionResponse.builder()
                    .id(t.getId())
                    .type(t.getType())
                    .amount(t.getAmount())
                    .paymentMethod(t.getPaymentMethod())
                    .status(t.getStatus())
                    .payosOrderCode(t.getPayosOrderCode())
                    .gatewayTransactionId(t.getGatewayTransactionId())
                    .description(t.getDescription())
                    .createdAt(t.getCreatedAt());

            if (t.getBooking() != null) {
                builder.bookingId(t.getBooking().getId());
                if (t.getBooking().getShop() != null) {
                    builder.shopName(t.getBooking().getShop().getShopName());
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
        }).collect(Collectors.toList());
    }
}
