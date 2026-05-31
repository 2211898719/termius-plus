package com.codeages.termiusplus.biz.patrol.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class JpaChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMemoryEntityRepository entityRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> findConversationIds() {
        return entityRepository.findAll().stream()
                .map(ChatMemoryEntity::getConversationId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        ChatMemoryEntity entity = entityRepository.findByConversationId(conversationId);
        if (entity == null || entity.getMessagesJson() == null) return Collections.emptyList();
        try {
            List<StoredMessage> stored = objectMapper.readValue(entity.getMessagesJson(), new TypeReference<>() {});
            return stored.stream().map(StoredMessage::toMessage).toList();
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat memory for conversation {}", conversationId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            List<StoredMessage> stored = messages.stream().map(StoredMessage::fromMessage).toList();
            String json = objectMapper.writeValueAsString(stored);
            ChatMemoryEntity entity = entityRepository.findByConversationId(conversationId);
            if (entity == null) {
                entity = new ChatMemoryEntity();
                entity.setConversationId(conversationId);
            }
            entity.setMessagesJson(json);
            entityRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat memory for conversation {}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        entityRepository.deleteByConversationId(conversationId);
    }

    // --- JPA Entity for chat memory ---

    @Entity
    @Data
    @NoArgsConstructor
    @EntityListeners(AuditingEntityListener.class)
    @jakarta.persistence.Table(name = "`patrol_chat_memory`")
    public static class ChatMemoryEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 36, unique = true)
        private String conversationId;

        @Column(columnDefinition = "MEDIUMTEXT")
        private String messagesJson;

        @CreatedDate
        private Long createdAt;
    }

    @Repository
    public interface ChatMemoryEntityRepository extends JpaRepository<ChatMemoryEntity, Long> {
        ChatMemoryEntity findByConversationId(String conversationId);
        void deleteByConversationId(String conversationId);
    }

    // --- Message serialization ---

    record StoredMessage(String role, String content) {
        static StoredMessage fromMessage(Message message) {
            return new StoredMessage(message.getMessageType().name(), message.getText());
        }

        Message toMessage() {
            return switch (MessageType.valueOf(role)) {
                case USER -> new org.springframework.ai.chat.messages.UserMessage(content);
                case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(content);
                case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(content);
                case TOOL -> org.springframework.ai.chat.messages.ToolResponseMessage.builder()
                        .responses(java.util.List.of(
                                new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(null, null, content)))
                        .build();
            };
        }
    }
}
