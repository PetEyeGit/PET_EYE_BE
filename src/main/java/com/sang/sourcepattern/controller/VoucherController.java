package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.VoucherCreationRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.entity.Voucher;
import com.sang.sourcepattern.entity.MembershipTier;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.MembershipTierRepository;
import com.sang.sourcepattern.repository.VoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/vouchers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class VoucherController {

    VoucherRepository voucherRepository;
    MembershipTierRepository membershipTierRepository;

    @GetMapping
    public ApiResponse<List<Voucher>> getAllVouchers() {
        return ApiResponse.<List<Voucher>>builder()
                .result(voucherRepository.findAll())
                .build();
    }

    @PostMapping
    public ApiResponse<Voucher> createVoucher(@RequestBody VoucherCreationRequest request) {
        MembershipTier targetTier = membershipTierRepository.findByName(request.getTargetTierName())
                .orElseGet(() -> membershipTierRepository.save(
                        MembershipTier.builder()
                                .name(request.getTargetTierName())
                                .requiredSpending(request.getRequiredSpending() != null ? request.getRequiredSpending() : 0.0)
                                .build()
                ));

        Voucher voucher = Voucher.builder()
                .code(request.getCode())
                .targetTier(targetTier)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .validDays(request.getValidDays())
                .issueQuantity(request.getIssueQuantity() != null ? request.getIssueQuantity() : 1)
                .build();

        return ApiResponse.<Voucher>builder()
                .result(voucherRepository.save(voucher))
                .message("Voucher created successfully")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<Voucher> updateVoucher(@PathVariable Integer id, @RequestBody VoucherCreationRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        MembershipTier targetTier = membershipTierRepository.findByName(request.getTargetTierName())
                .orElseGet(() -> membershipTierRepository.save(
                        MembershipTier.builder()
                                .name(request.getTargetTierName())
                                .requiredSpending(request.getRequiredSpending() != null ? request.getRequiredSpending() : 0.0)
                                .build()
                ));

        voucher.setCode(request.getCode());
        voucher.setTargetTier(targetTier);
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setValidDays(request.getValidDays());
        voucher.setIssueQuantity(request.getIssueQuantity() != null ? request.getIssueQuantity() : 1);

        return ApiResponse.<Voucher>builder()
                .result(voucherRepository.save(voucher))
                .message("Voucher updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteVoucher(@PathVariable Integer id) {
        voucherRepository.deleteById(id);
        return ApiResponse.<Void>builder()
                .message("Voucher deleted successfully")
                .build();
    }
}
