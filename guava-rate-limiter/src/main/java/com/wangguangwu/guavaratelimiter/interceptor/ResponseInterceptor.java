package com.wangguangwu.guavaratelimiter.interceptor;

import com.wangguangwu.guavaratelimiter.context.ResponseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Http 请求拦截器，用于在请求开始时绑定 HttpServletResponse 到 ThreadLocal，并在请求结束时清理。
 *
 * @author wangguangwu
 * @see ResponseContext
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
@Component
public class ResponseInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 将 response 绑定到 ThreadLocal
        ResponseContext.setResponse(response);
        // 继续处理请求
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal 中的 response
        ResponseContext.removeResponse();
    }
}
