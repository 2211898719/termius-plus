package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrolMessageRepository extends JpaRepository<PatrolMessage, Long> {
    List<PatrolMessage> findByConversationIdOrderBySortOrderAsc(String conversationId);
    void deleteByConversationId(String conversationId);
    int countByConversationId(String conversationId);
}
