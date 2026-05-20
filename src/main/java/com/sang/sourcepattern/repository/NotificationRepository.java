package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(int userId, Pageable pageable);
    long countByUserIdAndIsReadFalse(int userId);

    /** Lấy 1 đại diện mỗi broadcastId, sắp xếp mới nhất trước — có phân trang */
    @Query("SELECT n FROM Notification n WHERE n.id IN (" +
           "  SELECT MIN(n2.id) FROM Notification n2 GROUP BY n2.broadcastId" +
           ") ORDER BY n.createdAt DESC")
    Page<Notification> findDistinctBroadcasts(Pageable pageable);

    long countByBroadcastId(String broadcastId);
    long countByBroadcastIdAndIsReadTrue(String broadcastId);

    List<Notification> findByBroadcastId(String broadcastId);
    List<Notification> findByUserIdAndIsReadFalse(int userId);
    List<Notification> findByUserIdAndIsReadTrue(int userId);
}
