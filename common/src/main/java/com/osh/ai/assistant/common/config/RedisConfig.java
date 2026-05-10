package com.osh.ai.assistant.common.config;

import com.osh.ai.assistant.common.manager.CacheWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;


public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheWrapper cacheWrapper() {
        return new CacheWrapper();
    }

    /**
     * jsr107缓存抽象,org.springframework.boot.autoconfigure.cache.RedisCacheConfiguration中会注入
     * {@link RedisCacheManager},注入时使用{@link RedisCacheManagerBuilderCustomizer}定制.
     * 声明式缓存的使用:
     * 1.启用{@link org.springframework.cache.annotation.EnableCaching}
     * 2.使用@Cacheable等注解
     * 3.默认为jdk序列化,使用本方法进行定制{@link RedisCacheManager}的行为
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        return builder -> {
            RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                // 设置 Key 的序列化规则
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
                // 设置 Value 的序列化规则
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                ;
            builder.cacheDefaults(config);
        };
    }
}
