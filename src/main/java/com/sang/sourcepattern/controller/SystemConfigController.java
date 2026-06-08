package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.service.SystemConfigService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import com.sang.sourcepattern.dto.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemConfigController {

    SystemConfigService systemConfigService;

    // Public endpoint so users can check if the voucher service is enabled
    @GetMapping("/public/config/voucher-service")
    public ApiResponse<Map<String, Boolean>> getVoucherServiceConfig() {
        boolean enabled = systemConfigService.isVoucherServiceEnabled();
        return ApiResponse.<Map<String, Boolean>>builder()
                .result(Map.of("enabled", enabled))
                .build();
    }

    // Admin endpoint to toggle the voucher service
    @PutMapping("/admin/config/voucher-service")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Boolean>> setVoucherServiceConfig(@RequestParam boolean enabled) {
        systemConfigService.setVoucherServiceEnabled(enabled);
        return ApiResponse.<Map<String, Boolean>>builder()
                .result(Map.of("enabled", enabled))
                .build();
    }
}
