package com.wangguangwu.resilience4jratelimiter.controller;

import com.wangguangwu.resilience4jratelimiter.service.RateLimiterService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangguangwu
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @Resource
    private RateLimiterService apiRateLimiterService;

    @GetMapping("/rateLimiter")
    public String rateLimiter() {
        return apiRateLimiterService.doSomething();
    }
}
