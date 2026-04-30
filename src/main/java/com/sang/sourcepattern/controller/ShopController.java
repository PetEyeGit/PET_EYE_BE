package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.request.ShopUpdateRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.service.ShopService;
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
@RequestMapping("/shops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Shop Management")
public class ShopController {

    ShopService shopService;

    // ─── Shop Owner endpoints ────────────────────────────────────────────────

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Get the authenticated shop owner's shop profile")
    public ApiResponse<ShopResponse> getMyShop(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.getMyShop(jwt.getClaim("email")))
                .build();
    }

    @PutMapping("/my-shop")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Update the authenticated shop owner's shop profile")
    public ApiResponse<ShopResponse> updateMyShop(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ShopUpdateRequest request) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.updateMyShop(jwt.getClaim("email"), request))
                .message("Shop profile updated successfully")
                .build();
    }

    // ─── Public endpoints ────────────────────────────────────────────────────

    @GetMapping("/public")
    @Operation(summary = "Search verified shops (public) — keyword, city, shopType are optional")
    public ApiResponse<List<ShopResponse>> searchPublic(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String shopType) {
        return ApiResponse.<List<ShopResponse>>builder()
                .result(shopService.searchVerifiedShops(keyword, city, shopType))
                .build();
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Get a verified shop by id (public)")
    public ApiResponse<ShopResponse> getPublicById(@PathVariable int id) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.getVerifiedShopById(id))
                .build();
    }

    // ─── Registration ────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ApiResponse<ShopResponse> register(@RequestBody @Valid ShopRegistrationRequest request) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.registerShop(request))
                .build();
    }

    // ─── Admin endpoints ─────────────────────────────────────────────────────

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShopResponse> approve(@PathVariable int id) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.approveShop(id))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ShopResponse>> getAll() {
        return ApiResponse.<List<ShopResponse>>builder()
                .result(shopService.getAllShops())
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ShopResponse> getById(@PathVariable int id) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.getShopById(id))
                .build();
    }
}
