package com.wangguangwu.guavaratelimiter.controller;

import com.wangguangwu.guavaratelimiter.annotation.MyRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangguangwu
 */
@RestController
@Slf4j
@RequestMapping("/hello")
public class HelloController {

    /**
     * 通过注解实现限流
     */
    @MyRateLimiter(rate = 1.0, timeout = 500)
    @GetMapping("/world")
    public String message() {
        log.info("Hello World.");
        return "调用成功";
    }
}
