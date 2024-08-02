package com.wangguangwu.distributedratelimiter.context;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 使用 ThreadLocal 来管理 HttpServletResponse，以确保每个线程都能独立访问和管理自己的 HttpServletResponse 对象。
 *
 * @author wangguangwu
 */
public class ResponseContext {

    private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<>();

    public static void setResponse(HttpServletResponse response) {
        ResponseContext.RESPONSE_HOLDER.set(response);
    }

    public static HttpServletResponse getResponse() {
        return RESPONSE_HOLDER.get();
    }

    public static void removeResponse() {
        RESPONSE_HOLDER.remove();
    }
}
