package com.wangguangwu.distributedratelimiter.annotation;

import com.wangguangwu.distributedratelimiter.enums.LimitType;
import java.lang.annotation.*;

/**
 * 自定义限流注解，用于控制方法或类的访问频率。
 * <p>
 * 通过 Redis 和 Lua 脚本实现分布式限流。
 * 可以自定义限流的 key、前缀、时间范围、访问频率以及限流维度。
 * </p>
 *
 * @author wangguangwu
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DistributedRateLimiter {

    /**
     * 定义限流的唯一标识 key。
     * <p>
     * 可以用来区分不同的方法或资源，避免冲突。
     *
     * @return key 用于限流的唯一标识
     */
    String key();

    /**
     * 定义 key 的前缀。
     * <p>
     * 前缀用于区分不同业务或模块的限流 key。
     * 默认值为 "limiter:"。
     *
     * @return prefix key 的前缀
     */
    String prefix() default "limiter:";

    /**
     * 限流的时间范围，默认为 1 秒。
     * <p>
     * 该值表示在指定的时间范围内允许的最大请求次数。
     * 如果需要修改时间单位，需要同步修改对应的 Lua 脚本。
     *
     * @return period 限流的时间范围
     */
    int period() default 1;

    /**
     * 允许的最大访问次数，默认为 3 次。
     * <p>
     * 该值表示在指定的时间范围内，允许的最大请求次数。
     *
     * @return count 允许的最大访问次数
     */
    int count() default 3;

    /**
     * 限流的维度。
     * <p>
     * 可以根据 IP 地址进行限流，也可以根据自定义行为进行限流。
     * 默认值为 {@link LimitType#CUSTOMER}。
     *
     * @return limitType 限流的维度
     */
    LimitType limitType() default LimitType.CUSTOMER;

}
