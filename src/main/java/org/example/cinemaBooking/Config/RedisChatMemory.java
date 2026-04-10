package org.example.cinemaBooking.Config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class RedisChatMemory implements ChatMemory {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration chatMemoryTtl;

    public RedisChatMemory(RedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper,
                           Duration chatMemoryTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.chatMemoryTtl = chatMemoryTtl;
    }

    private String key(String conversationId) {
        return "chat:memory:" + conversationId;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        String redisKey = key(conversationId);
        for (Message message : messages) {
            try {
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList()
                        .rightPush(redisKey, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        if (!messages.isEmpty() && !chatMemoryTtl.isZero() && !chatMemoryTtl.isNegative()) {
            redisTemplate.expire(redisKey, chatMemoryTtl);
        }
    }

    // =========================
    // SAVE MESSAGES
    // =========================

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String redisKey = key(conversationId);
        Long size = redisTemplate.opsForList().size(redisKey);
        if (size == null || size == 0) {
            return List.of();
        }

        long start = 0;
        if (lastN > 0) {
            start = Math.max(0, size - lastN);
        }

        List<Object> raw = redisTemplate.opsForList()
                .range(redisKey, start, -1);

        if (raw == null) {
            return List.of();
        }

        return raw.stream()
                .map(obj -> {
                    try {
                        return objectMapper.readValue(
                                obj.toString(),
                                Message.class
                        );
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // =========================
    // CLEAR MEMORY
    // =========================
    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(key(conversationId));
    }
}