package com.wangguangwu.guavaratelimiter.aspect;

import com.wangguangwu.guavaratelimiter.annotation.MyRateLimiter;
import com.wangguangwu.guavaratelimiter.component.RateLimiterComponent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

/**
 * 实现自定义限流注解的切面。
 * <p>
 * 这个切面类使用 {@link RateLimiterComponent} 来控制请求的速率。
 * 如果请求的速率超过了限制，则会抛出 {@link RuntimeException}。
 * </p>
 *
 * @author wangguangwu
 * @see RateLimiterComponent
 * @see MyRateLimiter
 */
@Aspect
@Component
@Slf4j
public class MyRateLimiterAspect {

    @Resource
    private RateLimiterComponent rateLimiterComponent;

    @Pointcut("@annotation(myRateLimiter)")
    public void pointcut(MyRateLimiter myRateLimiter) {
    }

    @Around(value = "pointcut(myRateLimiter)", argNames = "joinPoint,myRateLimiter")
    public Object around(ProceedingJoinPoint joinPoint, MyRateLimiter myRateLimiter) throws Throwable {
        //
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String key = className + "." + methodName;

        // 获取速率和时间要求
        double rate = myRateLimiter.rate();
        int timeout = myRateLimiter.timeout();

        // 判断客户端获取令牌是否超时
        boolean tryAcquire = rateLimiterComponent.tryAcquire(key, rate, timeout);
        if (!tryAcquire) {
            // 服务降级
            return fullback();
        }

        // 获取到令牌，直接执行
        log.info("获取令牌成功，请求执行");
        return joinPoint.proceed();
    }

    /**
     * 降级处理
     */
    public Object fullback() {
        HttpServletResponse response = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
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
        return null;
    }
}
