package com.sang.sourcepattern.service;

import com.sang.sourcepattern.dto.request.ReviewRequest;
import com.sang.sourcepattern.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(String email, ReviewRequest request);
    List<ReviewResponse> getReviewsByShop(int shopId);
    List<ReviewResponse> getLatestReviews(int limit);
    ReviewResponse replyToReview(int reviewId, String reply, String ownerEmail);
}
