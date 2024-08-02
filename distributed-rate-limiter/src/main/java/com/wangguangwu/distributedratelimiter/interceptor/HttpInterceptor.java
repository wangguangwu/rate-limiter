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
 * 通过使用 ThreadLocal 将 HttpServletRequest 和 HttpServletResponse 与当前线程绑定，
 * 以便在应用程序的其他部分可以方便地访问这些对象。
 *
 * @author wangguangwu
 * @see ResponseContext
 * @see HandlerInterceptor
 */
@Component
public class HttpInterceptor implements HandlerInterceptor {

    /**
     * 在请求处理之前调用。用于将当前请求和响应对象与当前线程绑定。
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  chosen handler to execute, for type and/or instance evaluation
     * @return 是否成功
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        RequestContext.setRequest(request);
        ResponseContext.setResponse(response);
        return true;
    }

    /**
     * 在请求处理完成后调用，用于清理当前线程中绑定的请求和响应对象。
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  the handler (or {@link HandlerMethod}) that started asynchronous
     *                 execution, for type and/or instance examination
     * @param ex       any exception thrown on handler execution, if any; this does not
     *                 include exceptions that have been handled through an exception resolver
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        RequestContext.removeRequest();
        ResponseContext.removeResponse();
    }
}
