package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ReviewRequest;
import com.sang.sourcepattern.dto.response.PageResponse;
import com.sang.sourcepattern.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(String email, ReviewRequest request);
    List<ReviewResponse> getReviewsByShop(int shopId);
    PageResponse<ReviewResponse> getReviewsByShopPaged(int shopId, int page);
    long countReviewsByShop(int shopId);
    List<ReviewResponse> getLatestReviews(int limit);
    ReviewResponse replyToReview(int reviewId, String reply, String ownerEmail);
}
