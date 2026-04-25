package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.request.ShopRegistrationRequest;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.service.ShopService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopController {

    ShopService shopService;

    @PostMapping("/register")
    public ApiResponse<ShopResponse> register(@RequestBody @Valid ShopRegistrationRequest request) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.registerShop(request))
                .build();
    }

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
