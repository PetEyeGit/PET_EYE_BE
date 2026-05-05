package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByShopIdOrderByCreatedAtAsc(int shopId);
    long countByShopIdAndIsReadFalseAndSenderRoleNot(int shopId, String senderRole);
}
