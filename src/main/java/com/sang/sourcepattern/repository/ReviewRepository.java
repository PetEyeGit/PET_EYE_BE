package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByShopIdOrderByCreatedAtDesc(int shopId);
    long countByShopId(int shopId);
    boolean existsByUserIdAndShopId(int userId, int shopId);
}
