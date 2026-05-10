package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.AdminAIChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AdminAIChatHistoryRepository extends JpaRepository<AdminAIChatHistory, Integer> {

    List<AdminAIChatHistory> findByUserIdOrderByCreatedAtAsc(int userId);

    @Modifying
    @Transactional
    void deleteByUserId(int userId);

    long countByUserId(int userId);
}