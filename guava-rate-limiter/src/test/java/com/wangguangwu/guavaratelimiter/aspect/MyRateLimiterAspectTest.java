package com.wangguangwu.guavaratelimiter.aspect;

import com.wangguangwu.guavaratelimiter.component.RateLimiterComponent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MyRateLimiterAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimiterComponent rateLimiterComponent;

    @Test
    public void testRateLimiterAllowRequest() throws Exception {
        // Mock RateLimiter to allow the request
        when(rateLimiterComponent.tryAcquire(anyString(), anyDouble(), anyInt())).thenReturn(true);

        // Perform the request
        mockMvc.perform(get("/hello/world"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("调用成功")));

        // Verify that tryAcquire was called
        verify(rateLimiterComponent, times(1)).tryAcquire(anyString(), anyDouble(), anyInt());
    }

    @Test
    public void testRateLimiterDenyRequest() throws Exception {
        // Mock RateLimiter to deny the request
        when(rateLimiterComponent.tryAcquire(anyString(), anyDouble(), anyInt())).thenReturn(false);

        // Perform the request
        MvcResult result = mockMvc.perform(get("/hello/world"))
                .andExpect(status().isOk())
                .andReturn();

        // Verify the fallback response
        assertTrue(result.getResponse().getContentAsString().contains("服务出错，请稍后重试"));

        // Verify that tryAcquire was called
        verify(rateLimiterComponent, times(1)).tryAcquire(anyString(), anyDouble(), anyInt());
    }
}