package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.CareLogRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.CareLogResponse;
import com.sang.sourcepattern.service.CareLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/care-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Care Log Management", description = "APIs for Staff to record pet care activities")
public class CareLogController {

    CareLogService careLogService;

    @PostMapping("/{bookingId}")
    @PreAuthorize("hasRole('STAFF')")
    @Operation(summary = "Staff: Add a care log for a booking")
    public ApiResponse<CareLogResponse> addLog(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int bookingId,
            @RequestBody @Valid CareLogRequest request) {
        return ApiResponse.<CareLogResponse>builder()
                .result(careLogService.addLog(bookingId, request, jwt.getClaim("email")))
                .message("Care log added successfully")
                .build();
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('STAFF', 'CUSTOMER', 'SHOP_OWNER')")
    @Operation(summary = "Get all care logs for a booking")
    public ApiResponse<List<CareLogResponse>> getLogs(@PathVariable int bookingId) {
        return ApiResponse.<List<CareLogResponse>>builder()
                .result(careLogService.getLogsByBooking(bookingId))
                .build();
    }
}
