package com.wangguangwu.resilience4jratelimiter.service.impl;

import com.wangguangwu.resilience4jratelimiter.service.RateLimiterService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;

/**
 * 限流器。
 * <p>
 * 限制方法调用的速率，防止服务过载。
 *
 * @author wangguangwu
 */
@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    @Override
    @RateLimiter(name = "backend", fallbackMethod = "rateLimiterFallback")
    public String doSomething() {
        // 受限流器保护的方法逻辑
        return "调用成功!";
    }

    public String rateLimiterFallback(RequestNotPermitted ex) {
        return "Too many requests - Rate limit exceeded";
    }
}

