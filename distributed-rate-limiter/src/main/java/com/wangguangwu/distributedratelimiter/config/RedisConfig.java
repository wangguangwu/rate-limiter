package com.wangguangwu.distributedratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;

/**
 * Redis 配置类，用于配置 Redis 连接工厂和 RedisTemplate。
 * <p>
 * 该配置类定义了 Jedis 连接工厂和 RedisTemplate，用于在应用程序中操作 Redis。
 * </p>
 *
 * @author wangguangwu
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 Jedis 连接工厂，用于创建与 Redis 的连接。
     *
     * @return JedisConnectionFactory 实例
     */
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    /**
     * 配置 RedisTemplate，用于执行 Redis 操作。
     * <p>
     * 该 RedisTemplate 使用 StringRedisSerializer 序列化键，
     * 使用 GenericJackson2JsonRedisSerializer 序列化值，
     * 并设置连接工厂为 JedisConnectionFactory。
     * </p>
     *
     * @param redisConnectionFactory Redis 连接工厂，通常由 JedisConnectionFactory 提供
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Serializable> limitRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }
}
