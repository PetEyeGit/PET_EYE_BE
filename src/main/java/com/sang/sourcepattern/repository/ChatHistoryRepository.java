package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Integer> {

    /** Lấy toàn bộ lịch sử chat của user, sắp xếp theo thời gian tăng dần */
    List<ChatHistory> findByUserIdOrderByCreatedAtAsc(int userId);

    /** Xoá toàn bộ lịch sử của user */
    @Modifying
    @Transactional
    void deleteByUserId(int userId);

    /** Đếm số tin nhắn */
    long countByUserId(int userId);
}
