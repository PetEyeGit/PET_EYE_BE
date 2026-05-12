package com.sang.sourcepattern.controller;

import com.sang.sourcepattern.dto.request.ReviewReplyRequest;
import com.sang.sourcepattern.dto.request.ReviewRequest;
import com.sang.sourcepattern.dto.response.ApiResponse;
import com.sang.sourcepattern.dto.response.ReviewResponse;
import com.sang.sourcepattern.service.ReviewService;
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
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Review Management")
public class ReviewController {
    ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create a new review for a shop")
    public ApiResponse<ReviewResponse> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ReviewRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.createReview(jwt.getClaim("email"), request))
                .build();
    }

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "Get all reviews for a specific shop")
    public ApiResponse<List<ReviewResponse>> getReviewsByShop(@PathVariable int shopId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByShop(shopId))
                .build();
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @Operation(summary = "Shop owner replies to a review")
    public ApiResponse<ReviewResponse> replyToReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable int id,
            @RequestBody @Valid ReviewReplyRequest request) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.replyToReview(id, request.getReply(), jwt.getClaim("email")))
                .build();
    }
}
