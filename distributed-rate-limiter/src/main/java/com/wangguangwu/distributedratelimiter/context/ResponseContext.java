package com.wangguangwu.distributedratelimiter.context;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 使用 ThreadLocal 来管理 HttpServletResponse，以确保每个线程都能独立访问和管理自己的 HttpServletResponse 对象。
 * <p>
 * 该类提供了设置、获取和移除 HttpServletResponse 对象的静态方法。它常用于在多线程环境中需要安全地共享响应对象的场景，
 * 比如在过滤器、拦截器中设置响应对象，以便后续的业务逻辑能够访问和操作响应对象。
 * </p>
 *
 * @author wangguangwu
 */
public class ResponseContext {

    /**
     * 使用 ThreadLocal 存储每个线程独立的 HttpServletResponse 实例
     */
    private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的 HttpServletResponse 对象。
     *
     * @param response 当前线程的 HttpServletResponse 对象
     */
    public static void setResponse(HttpServletResponse response) {
        RESPONSE_HOLDER.set(response);
    }

    /**
     * 获取当前线程的 HttpServletResponse 对象。
     *
     * @return 当前线程的 HttpServletResponse 对象，如果没有设置则返回 null
     */
    public static HttpServletResponse getResponse() {
        return RESPONSE_HOLDER.get();
    }

    /**
     * 移除当前线程的 HttpServletResponse 对象。
     * <p>
     * 该方法通常在请求处理完成后调用，以避免内存泄漏。确保每个请求处理完后都清除与当前线程关联的响应对象。
     * </p>
     */
    public static void removeResponse() {
        RESPONSE_HOLDER.remove();
    }
}
