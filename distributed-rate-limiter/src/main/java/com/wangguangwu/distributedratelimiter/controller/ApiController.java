package com.wangguangwu.distributedratelimiter.controller;

import com.wangguangwu.distributedratelimiter.annotation.MyRateLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangguangwu
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/action")
    @MyRateLimiter(key = "action")
    public String action() {
        return "success";
    }
}