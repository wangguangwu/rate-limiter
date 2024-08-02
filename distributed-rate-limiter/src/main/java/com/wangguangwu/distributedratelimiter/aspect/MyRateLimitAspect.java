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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangguangwu
 */
@Aspect
@Component
@Slf4j
public class MyRateLimitAspect {

    private static final String UNKNOWN = "unknown";
    private static final String LIMIT_LUA_PATH = "limit.lua";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Integer> redisScript;

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        ;
        redisScript.setResultType(Integer.class);
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
        List<String> keys = new ArrayList<>();
        keys.add(StringUtils.join(myRateLimiter.prefix(), key));
        try {
            Integer count = stringRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
            log.info("key[{}]第[{}]次尝试获取令牌", key, count);
            if (count != null && count <= limitCount) {
                // 获取到令牌，直接执行
                log.info("获取令牌成功，请求执行");
                return joinPoint.proceed();
            } else {
                // 服务降级
                fallback();
                return null;
            }
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw new RuntimeException(e.getLocalizedMessage());
            }
            throw new RuntimeException("服务器出现异常，请稍后再试");
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
