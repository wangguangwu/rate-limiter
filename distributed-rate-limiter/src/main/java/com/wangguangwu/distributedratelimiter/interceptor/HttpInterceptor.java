package com.wangguangwu.distributedratelimiter.interceptor;

import com.wangguangwu.distributedratelimiter.context.RequestContext;
import com.wangguangwu.distributedratelimiter.context.ResponseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP 请求拦截器，用于在请求处理前后管理请求和响应对象的上下文。
 * <p>
 * 该拦截器通过使用 ThreadLocal 将 {@link HttpServletRequest} 和 {@link HttpServletResponse}
 * 与当前线程绑定，以便在应用程序的其他部分可以方便地访问这些对象。
 * </p>
 *
 * @author wangguangwu
 * @see RequestContext
 * @see ResponseContext
 * @see HandlerInterceptor
 */
@Component
public class HttpInterceptor implements HandlerInterceptor {

    /**
     * 在请求处理之前调用。用于将当前请求和响应对象与当前线程绑定。
     *
     * @param request  当前的 HTTP 请求
     * @param response 当前的 HTTP 响应
     * @param handler  处理器（或 {@link HandlerMethod}），用于处理当前请求
     * @return 返回 true 表示继续处理请求，返回 false 则表示中止请求
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // 将请求和响应对象与当前线程绑定
        RequestContext.setRequest(request);
        ResponseContext.setResponse(response);
        return true;
    }

    /**
     * 在请求处理完成后调用，用于清理当前线程中绑定的请求和响应对象。
     *
     * @param request  当前的 HTTP 请求
     * @param response 当前的 HTTP 响应
     * @param handler  处理器（或 {@link HandlerMethod}），用于处理当前请求
     * @param ex       处理过程中抛出的异常（如果有）
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        // 清理当前线程中的请求和响应对象，防止内存泄漏
        RequestContext.removeRequest();
        ResponseContext.removeResponse();
    }
}
