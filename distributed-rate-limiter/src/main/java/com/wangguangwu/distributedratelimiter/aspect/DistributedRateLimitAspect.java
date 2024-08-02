package com.wangguangwu.distributedratelimiter.aspect;

import com.wangguangwu.distributedratelimiter.annotation.DistributedRateLimiter;
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
 * 自定义切面，处理分布式限流注解 {@link DistributedRateLimiter}。
 * 通过 Lua 脚本在 Redis 中实现分布式限流。
 *
 * @author wangguangwu
 */
@Aspect
@Component
@Slf4j
public class DistributedRateLimitAspect {

    private static final String LIMIT_LUA_PATH = "limit.lua";

    @Resource
    private RedisTemplate<String, Serializable> limitRedisTemplate;

    private DefaultRedisScript<Long> redisScript;

    /**
     * 初始化方法，在 Bean 创建时加载 Lua 脚本。
     */
    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LIMIT_LUA_PATH)));
    }

    /**
     * 定义切点，匹配使用 {@link DistributedRateLimiter} 注解的方法。
     *
     * @param distributedRateLimiter 限流注解
     */
    @Pointcut("@annotation(distributedRateLimiter)")
    public void pointcut(DistributedRateLimiter distributedRateLimiter) {
    }

    /**
     * 环绕通知，处理限流逻辑。
     *
     * @param joinPoint              切入点
     * @param distributedRateLimiter 限流注解
     * @return 方法执行结果或降级处理结果
     */
    @Around(value = "pointcut(distributedRateLimiter)", argNames = "joinPoint,distributedRateLimiter")
    public Object around(ProceedingJoinPoint joinPoint, DistributedRateLimiter distributedRateLimiter) {
        // 获取速率和时间要求
        int limitPeriod = distributedRateLimiter.period();
        int limitCount = distributedRateLimiter.count();

        // 生成 Redis 键，区分限流类型
        String key = getKey(distributedRateLimiter.key(), distributedRateLimiter.limitType());
        String redisKey = StringUtils.join(distributedRateLimiter.prefix(), key);

        List<String> keys = Collections.singletonList(redisKey);

        try {
            Long result = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);

            // 判断是否获得令牌
            if (Boolean.TRUE.equals(result != null && result == 1)) {
                log.info("获取令牌成功，请求执行");
                return joinPoint.proceed();
            } else {
                // 服务降级处理
                fallback();
                return null;
            }
        } catch (Throwable e) {
            log.error("限流发生异常，走降级处理: {}", e.getMessage(), e);
            fallback();
            return null;
        }
    }

    /**
     * 降级处理方法。
     * 在限流条件触发时，返回错误信息给客户端。
     */
    private void fallback() {
        HttpServletResponse response = ResponseContext.getResponse();
        if (response != null) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                writer.println("服务出错，请稍后重试");
                writer.flush();
            } catch (IOException e) {
                log.error("服务降级: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 根据限流类型生成 Redis 键。
     *
     * @param customKey 自定义键
     * @param limitType 限流类型
     * @return 生成的 Redis 键
     */
    private String getKey(String customKey, LimitType limitType) {
        String key = switch (limitType) {
            case IP -> IpAddressUtil.getIpAddress();
            case CUSTOMER -> customKey;
        };
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("限流键不可为空");
        }
        return key;
    }
}
