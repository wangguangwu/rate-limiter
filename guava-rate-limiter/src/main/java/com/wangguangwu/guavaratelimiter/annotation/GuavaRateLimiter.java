package com.wangguangwu.guavaratelimiter.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解实现限流。
 * <p>
 * 用于定义方法级别的限流规则。
 * </p>
 *
 * @author wangguangwu
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GuavaRateLimiter {

    /**
     * 每秒的请求数
     *
     * @return rate
     */
    double rate() default 2.0;

    /**
     * 从令牌桶获取令牌的超时时间，默认不等待
     *
     * @return timeout
     */
    int timeout() default 0;

}
