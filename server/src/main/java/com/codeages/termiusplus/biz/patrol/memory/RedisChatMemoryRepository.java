package com.codeages.termiusplus.biz.patrol.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final long TTL_HOURS = 2;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) return Collections.emptyList();
        return keys.stream()
                   .map(k -> k.substring(KEY_PREFIX.length()))
                   .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + conversationId);
        if (json == null) return Collections.emptyList();
        try {
            List<StoredMessage> stored = objectMapper.readValue(json, new TypeReference<>() {});
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
            redisTemplate.opsForValue().set(KEY_PREFIX + conversationId, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat memory for conversation {}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

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
