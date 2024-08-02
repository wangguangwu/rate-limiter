package com.wangguangwu.guavaratelimiter.controller;

import com.wangguangwu.guavaratelimiter.annotation.GuavaRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangguangwu
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class ApiController {

    @GuavaRateLimiter(rate = 1.0, timeout = 500)
    @GetMapping("/action")
    public String action() {
        return "success";
    }
}
