package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawalRequestCreate {

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "10000", message = "Số tiền tối thiểu là 10,000đ")
    BigDecimal amount;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    String bankAccount;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    String accountHolder;

    String note;
}
