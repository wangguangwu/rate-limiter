package com.wangguangwu.distributedratelimiter.context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 使用 ThreadLocal 来管理 HttpServletRequest，以确保每个线程都能独立访问和管理自己的 HttpServletRequest 对象。
 *
 * @author wangguangwu
 */
public class RequestContext {

    private static final ThreadLocal<HttpServletRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    public static void setRequest(HttpServletRequest request) {
        REQUEST_THREAD_LOCAL.set(request);
    }

    public static HttpServletRequest getRequest() {
        return REQUEST_THREAD_LOCAL.get();
    }

    public static void removeRequest() {
        REQUEST_THREAD_LOCAL.remove();
    }

}
