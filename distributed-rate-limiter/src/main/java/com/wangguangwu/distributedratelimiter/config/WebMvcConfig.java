package com.wangguangwu.distributedratelimiter.config;

import com.wangguangwu.distributedratelimiter.interceptor.HttpInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置类，用于注册自定义的拦截器。
 * <p>
 * 通过实现 {@link WebMvcConfigurer} 接口，这个配置类可以对 Spring MVC 的默认配置进行扩展。
 * 例如，添加自定义的 {@link HandlerInterceptor} 实现，以便在处理请求之前或之后执行特定的逻辑。
 * </p>
 *
 * @author wangguangwu
 * @see HttpInterceptor
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 注册自定义的 {@link HandlerInterceptor} 实现到拦截器链中。
     * <p>
     * 该方法用于将自定义的拦截器 {@link HttpInterceptor} 添加到 Spring MVC 的拦截器链中，
     * 以便在请求处理的不同阶段执行预处理或后处理操作。
     * </p>
     *
     * @param registry 拦截器注册器对象，用于管理和添加拦截器。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 将自定义的 HttpInterceptor 添加到拦截器链中
        registry.addInterceptor(new HttpInterceptor());
    }
}
