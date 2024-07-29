package com.wangguangwu.resilience4jratelimiter.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangguangwu
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    @GetMapping("rateLimit")
    @RateLimiter(name = "rateLimitApi", fallbackMethod = "fallback")
    public ResponseEntity<String> rateLimitApi() {
        log.info("请求成功");
        return new ResponseEntity<>("请求成功", HttpStatus.OK);
    }

    public ResponseEntity<String> fallback(Throwable e) {
        log.error("请求失败: {}", e.getMessage(), e);
        return new ResponseEntity<>("请求过于频繁，请稍后再试", HttpStatus.OK);
    }
}
