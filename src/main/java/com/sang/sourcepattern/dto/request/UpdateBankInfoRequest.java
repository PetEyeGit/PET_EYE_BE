package com.sang.sourcepattern.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateBankInfoRequest {
    @NotBlank(message = "Bank name is required")
    String bankName;

    @NotBlank(message = "Bank account is required")
    String bankAccount;

    @NotBlank(message = "Account holder is required")
    String accountHolder;
}
