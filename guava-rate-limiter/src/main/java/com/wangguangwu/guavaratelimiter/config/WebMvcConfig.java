package com.wangguangwu.guavaratelimiter.config;

import com.wangguangwu.guavaratelimiter.interceptor.ResponseInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 配置类，用于注册拦截器。
 *
 * @author wangguangwu
 * @see ResponseInterceptor
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器
        registry.addInterceptor(new ResponseInterceptor());
    }
}
