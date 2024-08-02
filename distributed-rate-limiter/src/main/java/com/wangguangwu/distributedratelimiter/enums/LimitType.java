package com.wangguangwu.distributedratelimiter.enums;

/**
 * 枚举类，表示限流类型。
 * <p>
 * 用于指定限流的纬度，根据不同的需求，可以选择基于 IP 地址或自定义的限制方式。
 * </p>
 *
 * <p>例如：</p>
 * <ul>
 *     <li>{@link #IP} - 基于请求方的 IP 地址进行限流。</li>
 *     <li>{@link #CUSTOMER} - 基于自定义的标识符（如用户 ID）进行限流。</li>
 * </ul>
 *
 * @author wangguangwu
 */
public enum LimitType {

    /**
     * 基于请求方的 IP 地址进行限流。
     */
    IP,

    /**
     * 基于自定义的标识符（如用户 ID）进行限流。
     */
    CUSTOMER

}
