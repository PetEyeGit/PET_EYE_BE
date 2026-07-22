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
public class VoucherController {

    VoucherRepository voucherRepository;
    MembershipTierRepository membershipTierRepository;
    com.sang.sourcepattern.repository.UserVoucherRepository userVoucherRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Voucher>> getAllVouchers() {
        List<Voucher> vouchers = voucherRepository.findAll();
        vouchers.forEach(v -> v.setIssuedQuantity(userVoucherRepository.countByVoucherId(v.getId())));
        return ApiResponse.<List<Voucher>>builder()
                .result(vouchers)
                .build();
    }

    @GetMapping("/public")
    public ApiResponse<List<Voucher>> getPublicVouchers() {
        return ApiResponse.<List<Voucher>>builder()
                .result(voucherRepository.findAll())
                .build();
    }

    /**
     * Lay danh sach cac voucher NEWCOMER (danh cho tan thu).
     */
    @GetMapping("/newcomer")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Voucher>> getNewcomerVouchers() {
        return ApiResponse.<List<Voucher>>builder()
                .result(voucherRepository.findByVoucherType("NEWCOMER"))
                .message("Danh sach voucher tan thu")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Voucher> createVoucher(@RequestBody VoucherCreationRequest request) {
        String voucherType = (request.getVoucherType() != null && !request.getVoucherType().isBlank())
                ? request.getVoucherType().toUpperCase()
                : "TIER";

        MembershipTier targetTier = null;
        if ("TIER".equals(voucherType)) {
            if (request.getTargetTierName() == null || request.getTargetTierName().isBlank()) {
                throw new AppException(ErrorCode.INVALID_REQUEST);
            }
            targetTier = membershipTierRepository.findByName(request.getTargetTierName())
                    .orElseGet(() -> membershipTierRepository.save(
                            MembershipTier.builder()
                                    .name(request.getTargetTierName())
                                    .requiredSpending(request.getRequiredSpending() != null ? request.getRequiredSpending() : 0.0)
                                    .build()
                    ));
        }

        Voucher voucher = Voucher.builder()
                .code(request.getCode())
                .voucherType(voucherType)
                .targetTier(targetTier)
                .targetServiceCategory(request.getTargetServiceCategory())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue())
                .validDays(request.getValidDays() != null ? request.getValidDays() : 30)
                .issueQuantity(request.getIssueQuantity() != null ? request.getIssueQuantity() : 1)
                .build();

        return ApiResponse.<Voucher>builder()
                .result(voucherRepository.save(voucher))
                .message("Voucher created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Voucher> updateVoucher(@PathVariable Integer id, @RequestBody VoucherCreationRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        String voucherType = (request.getVoucherType() != null && !request.getVoucherType().isBlank())
                ? request.getVoucherType().toUpperCase()
                : voucher.getVoucherType();

        MembershipTier targetTier = voucher.getTargetTier();
        if ("TIER".equals(voucherType) && request.getTargetTierName() != null && !request.getTargetTierName().isBlank()) {
            targetTier = membershipTierRepository.findByName(request.getTargetTierName())
                    .orElseGet(() -> membershipTierRepository.save(
                            MembershipTier.builder()
                                    .name(request.getTargetTierName())
                                    .requiredSpending(request.getRequiredSpending() != null ? request.getRequiredSpending() : 0.0)
                                    .build()
                    ));
        } else if ("NEWCOMER".equals(voucherType)) {
            targetTier = null;
        }

        voucher.setCode(request.getCode());
        voucher.setVoucherType(voucherType);
        voucher.setTargetTier(targetTier);
        voucher.setTargetServiceCategory(request.getTargetServiceCategory());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(request.getMinOrderValue());
        if (request.getValidDays() != null) voucher.setValidDays(request.getValidDays());
        if (request.getIssueQuantity() != null) voucher.setIssueQuantity(request.getIssueQuantity());

        return ApiResponse.<Voucher>builder()
                .result(voucherRepository.save(voucher))
                .message("Voucher updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVoucher(@PathVariable Integer id) {
        voucherRepository.deleteById(id);
        return ApiResponse.<Void>builder()
                .message("Voucher deleted successfully")
                .build();
    }

    /**
     * Bat / tat tung voucher rieng le.
     * PATCH /admin/vouchers/{id}/toggle
     */
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Voucher> toggleVoucher(@PathVariable Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
        voucher.setActive(!voucher.isActive());
        Voucher saved = voucherRepository.save(voucher);
        String status = saved.isActive() ? "bat" : "tat";
        return ApiResponse.<Voucher>builder()
                .result(saved)
                .message("Da " + status + " voucher " + saved.getCode())
                .build();
    }
}