package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByShopIdAndChannelTypeOrderByCreatedAtAsc(int shopId, String channelType);
    
    // For direct messages (1-1)
    List<Message> findByShopIdAndChannelTypeAndSenderEmailAndRecipientEmailOrderByCreatedAtAsc(
            int shopId, String channelType, String senderEmail, String recipientEmail);

    List<Message> findByShopIdAndChannelTypeAndRecipientEmailOrderByCreatedAtAsc(
            int shopId, String channelType, String recipientEmail);
            
    long countByShopIdAndChannelTypeAndIsReadFalseAndSenderRoleNot(int shopId, String channelType, String senderRole);
}
