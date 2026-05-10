package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.ShopAIChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ShopAIChatHistoryRepository extends JpaRepository<ShopAIChatHistory, Integer> {

    List<ShopAIChatHistory> findByShopIdOrderByCreatedAtAsc(int shopId);

    @Modifying
    @Transactional
    void deleteByShopId(int shopId);

    long countByShopId(int shopId);
}