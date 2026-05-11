package com.sang.sourcepattern.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Yêu cầu rút tiền của Shop gửi lên Admin.
 * Trạng thái: PENDING → APPROVED | REJECTED
 */
@Entity
@Table(name = "withdrawal_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    Shop shop;

    /** Số tiền muốn rút */
    @Column(precision = 18, scale = 2)
    BigDecimal amount;

    /** Tên ngân hàng */
    String bankName;

    /** Số tài khoản ngân hàng */
    String bankAccount;

    /** Tên chủ tài khoản */
    String accountHolder;

    /** Ghi chú của shop */
    @Column(columnDefinition = "TEXT")
    String note;

    /** PENDING | APPROVED | REJECTED */
    @Builder.Default
    String status = "PENDING";

    /** Ghi chú của admin khi duyệt/từ chối */
    @Column(columnDefinition = "TEXT")
    String adminNote;

    /** PayOS order code dùng để chuyển tiền cho shop */
    Long payosOrderCode;

    /** PayOS checkout URL để admin thực hiện thanh toán */
    @Column(length = 1000)
    String checkoutUrl;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    LocalDateTime processedAt;
}
