package org.example.cinemaBooking.Config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(RedisTemplate<String, Object> redisTemplate,
                                 @Value("${app.chat-memory.ttl:PT24H}") Duration chatMemoryTtl) {
        return new RedisChatMemory(redisTemplate, RedisConfig.createRedisObjectMapper(), chatMemoryTtl);
    }
}