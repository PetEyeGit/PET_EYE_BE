package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByShopIdOrderByCreatedAtDesc(int shopId);
    Page<Review> findByShopIdOrderByCreatedAtDesc(int shopId, Pageable pageable);
    List<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByShopId(int shopId);
    boolean existsByUserIdAndShopId(int userId, int shopId);
}
