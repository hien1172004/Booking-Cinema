package org.example.cinemaBooking.Config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * Bean ObjectMapper dùng riêng cho Redis.
     * Đặt tên "redisObjectMapper" để tránh xung đột với ObjectMapper mặc định của Spring Boot.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        // Whitelist các package được phép deserialize — tránh deserialization attack
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.example.cinemaBooking")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(ptv,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * RedisTemplate dùng cho các thao tác thủ công với Redis (opsForValue, opsForHash, ...).
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory,
                                                       ObjectMapper redisObjectMapper) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager dùng cho @Cacheable, @CachePut, @CacheEvict.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                          ObjectMapper redisObjectMapper) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Cấu hình mặc định: TTL 1 giờ
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        // Cấu hình TTL riêng cho từng loại cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Thống kê doanh thu — làm mới thường xuyên (15 phút)
        cacheConfigurations.put("stats-summary",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("stats-revenue-chart",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("stats-ticket-chart",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("stats-top-movies",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Master data danh mục — ít thay đổi (24 giờ)
        cacheConfigurations.put("categories",
                defaultConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("category",
                defaultConfig.entryTtl(Duration.ofHours(24)));

        // Cấu hình cho ghế rạp chiếu và lịch (30 phút)
        cacheConfigurations.put("cinema-rooms", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("rooms", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }


}