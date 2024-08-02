package com.wangguangwu.distributedratelimiter.annotation;

import com.wangguangwu.distributedratelimiter.enums.LimitType;

import java.lang.annotation.*;

/**
 * 自定义限流注解。
 *
 * @author wangguangwu
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MyRateLimiter {

    /**
     * 缓存到Redis的key
     */
    String key();

    /**
     * Key的前缀
     */
    String prefix() default "limiter:";

    /**
     * 给定的时间范围 单位(秒)
     * 默认1秒 即1秒内超过count次的请求将会被限流
     */
    int period() default 1;

    /**
     * 一定时间内最多访问的次数
     */
    int count();

    /**
     * 限流的维度(用户自定义key 或者 调用方ip)
     */
    LimitType limitType() default LimitType.CUSTOMER;

}
