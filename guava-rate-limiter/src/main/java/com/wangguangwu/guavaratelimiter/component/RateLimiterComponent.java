package com.wangguangwu.guavaratelimiter.component;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流器组件。
 * </p>
 *
 * @author wangguangwu
 */
@Component
@Slf4j
public class RateLimiterComponent {

    /**
     * 为每一个接口创建自己的rateLimiter。
     * <p>
     * 避免并发问题。
     */
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    public RateLimiter getRateLimiter(String key, double rate) {
        return rateLimiterMap.computeIfAbsent(key, k -> RateLimiter.create(rate));
    }

    public boolean tryAcquire(String key, double rate, int timeout) {
        RateLimiter rateLimiter = getRateLimiter(key, rate);
        try {
            return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to acquire permission: {}", e.getMessage(), e);
            return false;
        }
    }
}
