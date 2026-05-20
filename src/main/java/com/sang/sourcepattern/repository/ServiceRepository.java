package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
    List<Service> findByShopId(int shopId);
    List<Service> findByShopIdAndActiveTrue(int shopId);
    List<Service> findByShopIdAndCategory(int shopId, String category);
    void deleteByShopId(int shopId);
}
