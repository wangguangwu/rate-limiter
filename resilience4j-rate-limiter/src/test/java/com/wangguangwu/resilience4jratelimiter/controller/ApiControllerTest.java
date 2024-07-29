package com.wangguangwu.resilience4jratelimiter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author wangguangwu
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(10);
    }

    @Test
    public void testConcurrentRateLimiter() throws Exception {
        int totalRequests = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    mockMvc.perform(get("/api/rateLimit"))
                            .andExpect(status().isOk())
                            .andDo(result -> {
                                if (result.getResponse().getContentAsString().contains("成功")) {
                                    successCount.incrementAndGet();
                                } else if (result.getResponse().getContentAsString().contains("频繁")) {
                                    failCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 所有任务同时开始
        startLatch.countDown();

        // 等待任务结束
        endLatch.await();

        System.out.println("Success Count: " + successCount.get());
        System.out.println("Fail Count: " + failCount.get());

        // Assert that rate limiter is working correctly
        assertTrue(successCount.get() <= 3, "Success count should be less than or equal to 3");
        assertTrue(failCount.get() >= 7, "Fail count should be greater than or equal to 7");

        // 等待限流结束
        TimeUnit.SECONDS.sleep(10);

        // Next request should succeed after refresh period
        mockMvc.perform(get("/api/rateLimit"))
                .andExpect(status().isOk());
    }
}