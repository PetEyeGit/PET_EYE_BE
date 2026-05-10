package com.sang.sourcepattern.repository.ai;

import com.sang.sourcepattern.entity.ai.AIMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessage, Long> {

    List<AIMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<AIMessage> findByOwnerKeyAndAgentTypeOrderByCreatedAtAsc(String ownerKey, String agentType);

    @Modifying
    @Transactional
    void deleteBySessionId(String sessionId);

    @Modifying
    @Transactional
    void deleteByOwnerKeyAndAgentType(String ownerKey, String agentType);

    long countBySessionId(String sessionId);
}
