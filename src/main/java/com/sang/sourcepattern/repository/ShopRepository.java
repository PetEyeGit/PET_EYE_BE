package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    List<Shop> findByOwnerId(int ownerId);
}
