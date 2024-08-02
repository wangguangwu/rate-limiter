package com.wangguangwu.distributedratelimiter.config;

import com.wangguangwu.distributedratelimiter.interceptor.HttpInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置类，用于注册拦截器。
 *
 * @author wangguangwu
 * @see HttpInterceptor
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 添加自定义的拦截器到 Spring MVC 的拦截器链中。
     * 这个方法用于注册自定义的 {@link HandlerInterceptor} 实现，以便在处理请求之前或之后执行特定的逻辑。
     *
     * @param registry 拦截器注册器对象，用于管理和添加拦截器。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HttpInterceptor());
    }
}
