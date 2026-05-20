package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Cage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CageRepository extends JpaRepository<Cage, Integer> {
    void deleteByShopId(int shopId);
}
