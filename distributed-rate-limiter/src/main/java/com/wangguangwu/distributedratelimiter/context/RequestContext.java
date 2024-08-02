package com.wangguangwu.distributedratelimiter.context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 使用 ThreadLocal 来管理 HttpServletRequest，以确保每个线程都能独立访问和管理自己的 HttpServletRequest 对象。
 * <p>
 * 该类提供了设置、获取和移除 HttpServletRequest 对象的静态方法，适用于需要在应用程序的不同层次中共享请求对象的场景。
 * 例如，在过滤器或拦截器中将请求对象设置到 ThreadLocal 中，以便在后续业务逻辑中可以随时访问。
 * </p>
 *
 * @author wangguangwu
 */
public class RequestContext {

    /**
     * 使用 ThreadLocal 来存储每个线程独立的 HttpServletRequest 实例
     */
    private static final ThreadLocal<HttpServletRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的 HttpServletRequest 对象。
     *
     * @param request 当前线程的 HttpServletRequest 对象
     */
    public static void setRequest(HttpServletRequest request) {
        REQUEST_THREAD_LOCAL.set(request);
    }

    /**
     * 获取当前线程的 HttpServletRequest 对象。
     *
     * @return 当前线程的 HttpServletRequest 对象，如果没有设置则返回 null
     */
    public static HttpServletRequest getRequest() {
        return REQUEST_THREAD_LOCAL.get();
    }

    /**
     * 移除当前线程的 HttpServletRequest 对象。
     * <p>
     * 该方法通常在请求处理完成后调用，以避免内存泄漏。
     * </p>
     */
    public static void removeRequest() {
        REQUEST_THREAD_LOCAL.remove();
    }
}
