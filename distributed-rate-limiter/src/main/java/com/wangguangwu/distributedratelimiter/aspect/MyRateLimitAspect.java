package com.wangguangwu.distributedratelimiter.aspect;

import com.wangguangwu.distributedratelimiter.annotation.MyRateLimiter;
import com.wangguangwu.distributedratelimiter.context.ResponseContext;
import com.wangguangwu.distributedratelimiter.enums.LimitType;
import com.wangguangwu.distributedratelimiter.util.IpAddressUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author wangguangwu
 */
@Aspect
@Component
@Slf4j
public class MyRateLimitAspect {

    private static final String LIMIT_LUA_PATH = "limit.lua";

    @Resource
    private RedisTemplate<String, Serializable> limitRedisTemplate;

    private DefaultRedisScript<Long> redisScript;

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LIMIT_LUA_PATH)));
    }

    @Pointcut("@annotation(myRateLimiter)")
    public void pointcut(MyRateLimiter myRateLimiter) {
    }

    @Around(value = "pointcut(myRateLimiter)", argNames = "joinPoint,myRateLimiter")
    public Object around(ProceedingJoinPoint joinPoint, MyRateLimiter myRateLimiter) {
        // 获取速率和时间要求
        int limitPeriod = myRateLimiter.period();
        int limitCount = myRateLimiter.count();

        // 获取对应的key
        String key = getKey(myRateLimiter.key(), myRateLimiter.limitType());
        String redisKey = StringUtils.join(myRateLimiter.prefix(), key);

        List<String> keys = Collections.singletonList(redisKey);

        try {
            Long result = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);

            if (Boolean.TRUE.equals(result != null && result == 1)) {
                // 获取到令牌，直接执行
                log.info("获取令牌成功，请求执行");
                return joinPoint.proceed();
            } else {
                // 服务降级处理
                fallback();
                return null;
            }
        } catch (Throwable e) {
            log.error("限流发生异常: {}", e.getMessage(), e);
            throw new RuntimeException("服务器出现异常，请稍后再试", e);
        }
    }

    /**
     * 降级处理
     */
    public void fallback() {
        HttpServletResponse response = ResponseContext.getResponse();
        if (response != null) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                log.info("服务出错，请稍后重试");
                writer.println("服务出错，请稍后重试");
                writer.flush();
            } catch (IOException e) {
                log.error("服务降级: {}", e.getMessage(), e);
            }
        }
    }

    private String getKey(String customKey, LimitType limitType) {
        String key = switch (limitType) {
            case IP -> IpAddressUtil.getIpAddress();
            case CUSTOMER -> customKey;
        };
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key不可为空");
        }
        return key;
    }
}
