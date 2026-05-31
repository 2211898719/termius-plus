package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.ChatMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMemoryEntityRepository extends JpaRepository<ChatMemoryEntity, Long> {
    ChatMemoryEntity findByConversationId(String conversationId);
    void deleteByConversationId(String conversationId);
}
