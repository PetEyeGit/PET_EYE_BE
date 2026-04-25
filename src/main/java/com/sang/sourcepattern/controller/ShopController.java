package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.ShopCreationRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ShopResponse;
import com.sang.sourcepattern.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Shop Management", description = "Endpoints for managing pet shops")
public class ShopController {
    ShopService shopService;

    @PostMapping
    @Operation(summary = "Create a new shop")
    ApiResponse<ShopResponse> createShop(@RequestBody @Valid ShopCreationRequest request) {
        return ApiResponse.<ShopResponse>builder()
                .result(shopService.createShop(request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all shops")
    ApiResponse<List<ShopResponse>> getAllShops() {
        return ApiResponse.<List<ShopResponse>>builder()
                .result(shopService.getAllShopResponses())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shop by ID")
    ApiResponse<ShopResponse> getShop(@PathVariable int id) {
        // Implementation here if needed, or just use service
        return null; 
    }
}
