package com.sang.sourcepattern.repository;

import com.sang.sourcepattern.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m.shopId FROM Message m WHERE m.senderEmail = :email OR m.recipientEmail = :email")
    List<Integer> findShopIdsByParticipantEmail(@org.springframework.data.repository.query.Param("email") String email);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.shopId = :shopId AND m.channelType = :channelType AND m.senderRole <> :readerRole AND m.isRead = false")
    void markAllAsRead(@Param("shopId") int shopId, @Param("channelType") String channelType, @Param("readerRole") String readerRole);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.shopId = :shopId AND m.channelType = :channelType AND m.recipientEmail = :recipientEmail AND m.senderRole <> :readerRole AND m.isRead = false")
    void markRecipientAllAsRead(@Param("shopId") int shopId, @Param("channelType") String channelType, @Param("recipientEmail") String recipientEmail, @Param("readerRole") String readerRole);
}
