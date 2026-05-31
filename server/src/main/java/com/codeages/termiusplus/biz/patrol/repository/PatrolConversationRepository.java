package com.codeages.termiusplus.biz.patrol.repository;

import com.codeages.termiusplus.biz.patrol.entity.PatrolConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatrolConversationRepository extends JpaRepository<PatrolConversation, Long> {
    Optional<PatrolConversation> findByConversationId(String conversationId);
    void deleteByConversationId(String conversationId);
}
