package com.sang.sourcepattern.service.impl;

import com.sang.sourcepattern.dto.request.ReviewRequest;
import com.sang.sourcepattern.dto.response.ReviewResponse;
import com.sang.sourcepattern.entity.Review;
import com.sang.sourcepattern.entity.Shop;
import com.sang.sourcepattern.entity.User;
import com.sang.sourcepattern.exception.AppException;
import com.sang.sourcepattern.exception.ErrorCode;
import com.sang.sourcepattern.repository.BookingRepository;
import com.sang.sourcepattern.repository.ReviewRepository;
import com.sang.sourcepattern.repository.ShopRepository;
import com.sang.sourcepattern.repository.UserRepository;
import com.sang.sourcepattern.service.ReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    ReviewRepository reviewRepository;
    ShopRepository shopRepository;
    UserRepository userRepository;
    BookingRepository bookingRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(String email, ReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        // Logic: Verify the specific booking
        com.sang.sourcepattern.entity.Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (booking.getUser().getId() != user.getId() || booking.getShop().getId() != shop.getId()) {
            throw new AppException(ErrorCode.BOOKING_NOT_BELONG_TO_USER);
        }

        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // Anti-spam: Check if user already reviewed this shop (keep 1 per shop for now)
        if (reviewRepository.existsByUserIdAndShopId(user.getId(), shop.getId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTED);
        }

        Review review = Review.builder()
                .user(user)
                .shop(shop)
                .service(booking.getService()) // Link the service
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);
        updateShopRating(shop);

        return toReviewResponse(review);
    }

    @Override
    public List<ReviewResponse> getReviewsByShop(int shopId) {
        return reviewRepository.findByShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getLatestReviews(int limit) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(int reviewId, String reply, String ownerEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND)); // Use better error if needed

        Shop shop = review.getShop();
        if (!shop.getOwner().getEmail().equals(ownerEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        review.setReply(reply);
        review.setRepliedAt(java.time.LocalDateTime.now());

        return toReviewResponse(reviewRepository.save(review));
    }

    private void updateShopRating(Shop shop) {
        List<Review> reviews = reviewRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        if (reviews.isEmpty()) {
            shop.setRatingAvg(0.0f);
        } else {
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            shop.setRatingAvg((float) avg);
        }
        shopRepository.save(shop);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userName(review.getUser().getFullName())
                .userAvatar(review.getUser().getAvatar())
                .serviceName(review.getService() != null ? review.getService().getServiceName() : null)
                .shopName(review.getShop().getShopName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .reply(review.getReply())
                .repliedAt(review.getRepliedAt())
                .build();
    }
}
