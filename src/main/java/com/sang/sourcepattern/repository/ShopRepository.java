package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    Optional<Shop> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Shop> findByOwnerId(int ownerId);
}
