package com.wangguangwu.guavaratelimiter.context;

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

    public static ThreadLocal<HttpServletResponse> getResponseHolder() {
        return RESPONSE_HOLDER;
    }

    public static void removeResponse() {
        RESPONSE_HOLDER.remove();
    }
}
