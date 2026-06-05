package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    Optional<Shop> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Shop> findFirstByOwnerId(int ownerId);
    Optional<Shop> findFirstByOwnerEmail(String email);

    default Optional<Shop> findByOwnerId(int ownerId) {
        return findFirstByOwnerId(ownerId);
    }

    default Optional<Shop> findByOwnerEmail(String email) {
        return findFirstByOwnerEmail(email);
    }

    @Query("""
        SELECT DISTINCT s FROM Shop s
        LEFT JOIN FETCH s.services svc
        WHERE s.isVerified = true
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(s.shopName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(svc.serviceName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:city IS NULL OR :city = ''
               OR LOWER(s.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:shopType IS NULL OR :shopType = ''
               OR LOWER(s.shopType) = LOWER(:shopType)
               OR LOWER(s.shopType) = 'mixed')
        ORDER BY s.ratingAvg DESC
    """)
    List<Shop> searchVerified(
            @Param("keyword") String keyword,
            @Param("city") String city,
            @Param("shopType") String shopType
    );
}
