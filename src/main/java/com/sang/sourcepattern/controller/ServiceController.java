package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.ServiceCreationRequest;
import com.sang.sourcepattern.dto.request.ServiceUpdateRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ServiceResponse;
import com.sang.sourcepattern.service.ServiceService;
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
@RequestMapping("/services")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Service Management", description = "APIs for shop owners to manage their services")
public class ServiceController {

    ServiceService serviceService;

    // ─── Shop Owner endpoints ────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Create a new service (Shop Owner only)")
    public ApiResponse<ServiceResponse> createService(
            @RequestBody @Valid ServiceCreationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaim("email");
        return ApiResponse.<ServiceResponse>builder()
                .result(serviceService.createService(request, email))
                .message("Service created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Update a service (Shop Owner only)")
    public ApiResponse<ServiceResponse> updateService(
            @PathVariable int id,
            @RequestBody @Valid ServiceUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaim("email");
        return ApiResponse.<ServiceResponse>builder()
                .result(serviceService.updateService(id, request, email))
                .message("Service updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Deactivate a service (Shop Owner only)")
    public ApiResponse<Void> deleteService(
            @PathVariable int id,
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaim("email");
        serviceService.deleteService(id, email);
        return ApiResponse.<Void>builder()
                .message("Service deactivated successfully")
                .build();
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Get all services of the owner's shop (including inactive)")
    public ApiResponse<List<ServiceResponse>> getMyShopServices(
            @AuthenticationPrincipal Jwt jwt) {

        String email = jwt.getClaim("email");
        return ApiResponse.<List<ServiceResponse>>builder()
                .result(serviceService.getMyShopServices(email))
                .build();
    }

    // ─── Public / Customer endpoints ─────────────────────────────────────────

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "Get active services for a shop (public)")
    public ApiResponse<List<ServiceResponse>> getServicesByShop(@PathVariable int shopId) {
        return ApiResponse.<List<ServiceResponse>>builder()
                .result(serviceService.getServicesByShop(shopId))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service detail by id (public)")
    public ApiResponse<ServiceResponse> getServiceById(@PathVariable int id) {
        return ApiResponse.<ServiceResponse>builder()
                .result(serviceService.getServiceById(id))
                .build();
    }
}
