package org.example.cinemaBooking.Config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Mặc định cache tồn tại 1 giờ
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Thiết lập TTL riêng cho từng loại cache (Optional)
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Cấu hình riêng cho Thống kê (Statistics) - thời gian sống ngắn (15 phút) để quét liên tục doanh thu
        cacheConfigurations.put("stats-summary", config.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("stats-revenue-chart", config.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("stats-ticket-chart", config.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("stats-top-movies", config.entryTtl(Duration.ofMinutes(15)));

        // Cấu hình dài hạn (24 giờ) cho các Master Data như Danh mục (Categories) tránh quét nhiều do ít đổi
        cacheConfigurations.put("categories", config.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("category", config.entryTtl(Duration.ofHours(24)));

        // Cấu hình cho ghế rạp chiếu và lịch (30 phút)
        cacheConfigurations.put("cinema-rooms", config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("rooms", config.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}