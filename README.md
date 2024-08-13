# rate-limiter

- guava-rate-limiter: 使用guava实现单机限流。
- resilience4j-rate-limiter: 使用resilience4j实现单机限流。
- distributed-rate-limiter: 使用redis+lua实现分布式限流。

[文章地址](https://www.wangguangwu.com/archives/68622083-95be-41ae-b520-1dc3ac778d65)

### 参考文献

[高性能分布式限流：Redis+Lua 真香！](https://cloud.tencent.com/developer/article/2241215)

# 1. 基础概念

分布式系统高可用的 4 大手段：

重试、限流、熔断、降级。

## 1.1 什么是限流

限流是一种**流量控制技术**，用于**限制系统在单位时间内处理的请求数量，以防止系统过载和崩溃**。

## 1.2 为什么需要限流

保护系统的稳定性和可用性。

防止突发流量或恶意请求导致系统资源耗尽，从而引发服务不可用或性能下降的问题。

## 1.3 限流的使用场景

- 对稀缺资源的秒杀、抢购；
- 对数据库的高并发读写操作，比如提交订单，瞬间插入大量数据。

## 1.4 限流的优缺点

**优点**：

1. **保护系统**：防止系统过载，保证服务稳定性和可用性。
2. **资源管理**：有效分配和使用系统资源，避免资源争夺。
3. **公平性**：在流量高峰期，确保每个用户都有公平的访问机会。

**缺点**：

1. **请求延迟**：可能引发请求排队或延迟，影响用户体验。
2. **误杀流量**：限流策略过于严格时，可能误拒合法请求。
3. **复杂度**：实现和维护限流机制需要一定的开发和运维成本。

## 1.5 有没有其他的方式可以达成一样的效果

- **负载均衡**：分配请求到多台服务器，减少单点压力。
- **缓存**：使用缓存减少数据库或服务的压力，提高响应速度。
- **熔断机制**：在高负载或错误率高时，自动停止调用故障服务，防止系统崩溃。
- **降级策略**：在系统负载过高时，降级部分服务功能，以确保核心功能可用。

这些方法可以与限流结合使用，共同保证系统的高可用性和稳定性。

# 2. 常用算法

## 2.1 计算器法（Fixed Window Counter）

### 2.1.1 基本概念

一种简单的限流算法，在固定时间窗口内计数请求数量，当请求数量达到设定的阈值时拒绝后续请求。

**核心**：单位时间内直接计数。

### 2.1.2 适用场景

适用于**请求分布均匀**的场景，例如每分钟允许固定数量的 API 请求。

### 2.1.3 优缺点

**优点**：

- 简单易实现；
- 计算和存储开销小。

**缺点**：

- **边界问题明显**：在窗口切换时，可能在短时间内处理大量请求，导致**突刺问题**。

### 2.1.4 Java 代码实现

```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 - Fixed Window Counter.
 *
 - @author wangguangwu
 */
 public class FixedWindowCounter {

    private final int limit;
    private final long windowSize;
    private final AtomicInteger counter;
    private long windowStart;

    public FixedWindowCounter(int limit, long windowSize) {
        this.limit = limit;
        this.windowSize = windowSize;
        this.counter = new AtomicInteger(0);
        this.windowStart = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        if (now - windowStart > windowSize) {
            counter.set(0);
            windowStart = now;
        }

        return counter.incrementAndGet() <= limit;
    }

    public static void main(String[] args) {
        // Set the counter
        FixedWindowCounter counter = new FixedWindowCounter(5, 1000);

        // Simulate request
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (counter.allowRequest()) {
                        System.out.println(Thread.currentThread().getName() + ": Request allowed at " + System.currentTimeMillis());
                    } else {
                        System.out.println(Thread.currentThread().getName() + ": Request denied at " + System.currentTimeMillis());
                    }

                    try {
                        TimeUnit.MICROSECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }
}
```

## 2.2 滑动时间窗口（**Sliding Window Counter**）

### 2.2.1 基本概念

滑动窗口算法记录每个请求的时间戳，并在一个滚动时间窗口内检查请求数量，超过限额则拒绝请求。

### 2.2.2 适用场景

适用于需要精确控制请求速率的场景。

### 2.2.3 优缺点

**优点**：

- 解决固定窗口的边界问题；
- 更平滑的限流效果。

**缺点**：

- 存储和计算开销大，需要记录每个请求的时间戳。

### 2.2.4 Java 代码实现

```java
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 - Sliding window counter.
 *
 - @author wangguangwu
 */
 public class SlidingWindowCounter {

    private final int limit;
    private final int windowSize;
    private final Queue<Long> queue;

    public SlidingWindowCounter(int limit, int windowSize) {
        this.windowSize = windowSize;
        this.limit = limit;
        this.queue = new LinkedList<>();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        while (!queue.isEmpty() && now - queue.peek() > windowSize) {
            queue.poll();
        }

        if (queue.size() < limit) {
            queue.add(now);
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        // Set the counter
        SlidingWindowCounter counter = new SlidingWindowCounter(5, 1000);

        // Simulate requests
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (counter.allowRequest()) {
                        System.out.println(Thread.currentThread().getName() + ": Request allowed at " + System.currentTimeMillis());
                    } else {
                        System.out.println(Thread.currentThread().getName() + ": Request denied at " + System.currentTimeMillis());
                    }

                    try {
                        // Simulate request interval
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }
}
```

## 2.3 **漏桶算法（Leaky Bucket）**

### 2.3.1 基本概念

漏桶算法将请求放入漏桶中，漏桶以固定速率漏水（处理请求），当漏桶满时拒绝新请求。

**核心**：是否有容量。

### 2.3.2 适用场景

适用于流量整形，平滑突发流量的场景。

### 2.3.3 优缺点

**优点**：

- 平滑突发流量；
- 简单易实现。

**缺点**：

- 固定漏水速率，灵活性较差。

### 2.3.4 Java 代码实现

```java
import java.util.concurrent.TimeUnit;

/**
 - Leaky bucket.
 *
 - @author wangguangwu
 */
 public class LeakyBucket {

    private final int capacity;
    private final int rate;
    private int water;
    private long lastLeakTime;

    public LeakyBucket(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.water = 0;
        this.lastLeakTime = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastLeakTime;
        int leaked = (int) (elapsedTime * rate / 1000);

        water = Math.max(0, water - leaked);
        lastLeakTime = now;
        if (water < capacity) {
            water++;
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        // Set the leaky bucket
        LeakyBucket bucket = new LeakyBucket(5, 1);

        // Simulate requests
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (bucket.allowRequest()) {
                        System.out.println(Thread.currentThread().getName() + ": Request allowed at " + System.currentTimeMillis());
                    } else {
                        System.out.println(Thread.currentThread().getName() + ": Request denied at " + System.currentTimeMillis());
                    }

                    try {
                        // Simulate request interval
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }
}
```

## 2.4 **令牌桶算法（Token Bucket）**

### 2.4.1 基本概念

令牌桶算法以固定速率生成令牌，放入桶中，请求需**获取令牌**才能通过，桶满时停止生成新令牌。

**核心**：是否有令牌。

### 2.4.2 适用场景

适用于处理突发流量，同时控制整体速率的场景。

### 2.4.3 优缺点

**优点**：

- 允许一定的突发流量；
- 灵活控制流量。

**缺点**：

- 实现相对复杂。

### 2.4.4 Java 代码实现

```java
import java.util.concurrent.TimeUnit;

/**
 - Token Bucket.
 *
 - @author wangguangwu
 */
 public class TokenBucket {

    private final int capacity;
    private final int rate;
    private int tokens;
    private long lastTokenTime;

    public TokenBucket(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        this.tokens = capacity;
        this.lastTokenTime = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastTokenTime;
        int newTokens = (int) (elapsedTime * rate / 1000);
        tokens = Math.min(capacity, tokens + newTokens);
        lastTokenTime = now;
        if (tokens > 0) {
            tokens--;
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        // Set the token bucket
        TokenBucket bucket = new TokenBucket(5, 1);

        // Simulate requests
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (bucket.allowRequest()) {
                        System.out.println(Thread.currentThread().getName() + ": Request allowed at " + System.currentTimeMillis());
                    } else {
                        System.out.println(Thread.currentThread().getName() + ": Request denied at " + System.currentTimeMillis());
                    }

                    try {
                        // Simulate request interval
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
    }
}
```

## 2.5 边界问题

### 2.5.1 示例

假设一个计数器法限流器的配置如下：

• **限制**：每秒最多允许 100 个请求；
• **时间窗口**：1 秒（1000 毫秒）。

问题发生在窗口边界处，例如在时间窗口的末尾和下一个时间窗口的开始之间。

假设当前时间是 T 毫秒，在时间窗口 T 毫秒到 T+1000 毫秒内最多允许 100 个请求：

1. **第一个时间窗口**：

- 从 T 到 T+1000 毫秒内，允许最多 100 个请求。
- 到 T+1000 毫秒时，计数器重置为 0。

2. **第二个时间窗口**：

- 从 T+1000 到 T+2000 毫秒内，允许最多 100 个请求。

假设在时间 T+990 毫秒到 T+1000 毫秒之间，有 100 个请求进来，这些请求会被计数器法限流器允许。在时间 T+1000 毫秒之后，计数器重置为 0，如果在时间 T+1000 毫秒到 T+1010 毫秒之间，又有 100 个请求进来，这些请求也会被计数器法限流器允许。

这样，在短短的 20 毫秒内，系统实际上处理了 200 个请求，超过了每秒 100 个请求的限制。这种情况在窗口切换时非常常见，称为**突刺问题**。

### 2.5.2 影响

1. **瞬时过载**：大量请求在短时间内涌入，可能导致系统瞬时过载，影响系统的稳定性和性能。
2. **不公平性**：某些时间段内请求被大量允许，而其他时间段内请求可能被拒绝，导致请求处理的不公平性。

### 2.5.3 解决方法

为了缓解计数器法的边界问题，可以采用以下几种改进方法：

**1. 滑动窗口计数器**

滑动窗口计数器通过将时间窗口进一步划分为多个小窗口，记录每个小窗口的请求数量，然后在每个请求到达时根据当前时间计算滑动窗口内的请求总量。

**2. 漏桶算法（Leaky Bucket）**

漏桶算法将请求放入漏桶中，以固定速率处理请求，平滑流量，避免突发流量。

**3. 令牌桶算法（Token Bucket）**

令牌桶算法以固定速率生成令牌，请求需获取令牌才能通过，允许一定的突发流量，同时控制整体速率。

# 3. 代码实现

在限流处理中，主要有三种方式可供选择：

1. **guava-rate-limiter**：使用 Guava 实现限流，适合单机环境。
2. **resilience4j-rate-limiter**：使用 Resilience4j 实现限流，适合单机环境。
3. **distributed-rate-limiter**：使用 Redis + Lua 脚本实现限流，适合分布式环境。

## 3.1 guava-rate-limiter

### 3.1.1 结构

```txt
└── com
     └── wangguangwu
         └── guavaratelimiter
             ├── GuavaRateLimiterApplication.java
             ├── annotation
             │   └── GuavaRateLimiter.java
             ├── aspect
             │   └── GuavaRateLimiterAspect.java
             ├── component
             │   └── RateLimiterComponent.java
             └── controller
                 └── ApiController.java
```

### 3.1.2 依赖及配置项

在 pom.xml 中引入 guava 和 SpringBoot AOP 的依赖：

ps：如果不想通过注解实现，可以去除 SpringBoot AOP 的依赖。

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>${guava.version}</version>
</dependency>

<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 3.1.3 核心代码

**GuavaRateLimiter**：自定义注解实现限流。

```java
import java.lang.annotation.*;

/**
 - 自定义注解实现限流。
 - <p>
 - 用于定义方法级别的限流规则。
 - </p>
 *
 - @author wangguangwu
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GuavaRateLimiter {

    /**
     - 每秒的请求数
     *
     - @return rate
     */
     double rate() default 2.0;

    /**
     - 从令牌桶获取令牌的超时时间，默认不等待
     *
     - @return timeout
     */
     int timeout() default 0;

}
```

**GuavaRateLimiterAspect**：实现自定义限流注解的切面。

```java
import com.wangguangwu.guavaratelimiter.annotation.GuavaRateLimiter;
import com.wangguangwu.guavaratelimiter.component.RateLimiterComponent;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

/**
 - 实现自定义限流注解的切面。
 - <p>
 - 这个切面类使用 {@link RateLimiterComponent} 来控制请求的速率。
 - 如果请求的速率超过了限制，则会抛出 {@link RuntimeException}。
 - </p>
 *
 - @author wangguangwu
 - @see RateLimiterComponent
 - @see GuavaRateLimiter
 */
@Aspect
@Component
@Slf4j
public class GuavaRateLimiterAspect {

    @Resource
    private RateLimiterComponent rateLimiterComponent;

    @Pointcut("@annotation(guavaRateLimiter)")
    public void pointcut(GuavaRateLimiter guavaRateLimiter) {
    }

    @Around(value = "pointcut(guavaRateLimiter)", argNames = "joinPoint,guavaRateLimiter")
    public Object around(ProceedingJoinPoint joinPoint, GuavaRateLimiter guavaRateLimiter) throws Throwable {
        // 获取类名称 + 方法名称
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String key = className + "." + methodName;

        // 获取速率和时间要求
        double rate = guavaRateLimiter.rate();
        int timeout = guavaRateLimiter.timeout();

        // 判断客户端获取令牌是否超时
        boolean tryAcquire = rateLimiterComponent.tryAcquire(key, rate, timeout);
        if (!tryAcquire) {
            // 服务降级
            fallback();
            return null;
        }

        // 获取到令牌，直接执行
        log.info("获取令牌成功，请求执行");
        return joinPoint.proceed();
    }

    /**
     - 降级处理
     */
    public void fallback() {
        HttpServletResponse response = ((ServletRequestAttributes)
                Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
        if (response != null) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                log.info("服务出错，请稍后重试");
                writer.println("服务出错，请稍后重试");
                writer.flush();
            } catch (IOException e) {
                log.error("服务降级: {}", e.getMessage(), e);
            }
        }
    }
}
```

**RateLimiterComponent**：限流器组件。

```java
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 - 限流器组件。
 - </p>
 *
 - @author wangguangwu
 */@Component
@Slf4j
@SuppressWarnings("all")
public class RateLimiterComponent {

    /**
     - 为每一个接口创建自己的 rateLimiter。
     - <p>
     - 避免并发问题。
     */
    private final ConcurrentHashMap<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    public RateLimiter getRateLimiter(String key, double rate) {
        return rateLimiterMap.computeIfAbsent(key, k -> RateLimiter.create(rate));
    }

    public boolean tryAcquire(String key, double rate, int timeout) {
        RateLimiter rateLimiter = getRateLimiter(key, rate);
        try {
            return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to acquire permission: {}", e.getMessage(), e);
            return false;
        }
    }
}
```

## 3.2 resilience4j -rate-limiter

### 3.2.1 结构

```txt
└── com
    └── wangguangwu
        └── resilience4jratelimiter
            ├── Resilience4jRateLimiterApplication.java
            └── controller
                └── ApiController.java
```

### 3.2.2 依赖及配置项

#### 3.2.2.1 引入依赖

在 pom.xml 中引入 Resilience4j 和 SpringBoot AOP 的依赖：

ps：`resilience4j`通过 AOP 实现功能，两者需要同时引入。

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>${resilience4j.version}</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

#### 3.2.2.2 配置项

在 application.properties 中添加 Resilience4j 的限流配置：

```properties
# Resilience4j RateLimiter
# 每个时间段的请求限制数量，即在每个刷新周期内允许通过的最大请求数
resilience4j.ratelimiter.instances.rateLimitApi.limit-for-period=3
# 限流器的刷新周期，每隔这个时间段，限流器会重置已通过的请求计数
resilience4j.ratelimiter.instances.rateLimitApi.limit-refresh-period=1s
# 获取许可的超时时间，若在指定时间内无法获取许可，则请求会被拒绝
resilience4j.ratelimiter.instances.rateLimitApi.timeout-duration=500ms
```

### 3.2.3 核心代码

ApiController：控制器类。

```java
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 - 该控制器实现了基于 Resilience4j 的限流功能。
 - 当请求过于频繁时，调用回退方法返回提示信息。
 *
 - @author wangguangwu
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
```

## 3.3 distributed-rate-limiter

分布式限流方式：

1. 网关层限流。将限流规则应用在所有流量的入口处。
2. 中间件限流。将限流信息存储在分布式环境中某个中间件里（比如 redis），每个组件都可以从这里获取到当前时间的流量统计，从而决定是否放行还是拒绝。

### 3.3.1 结构

```txt
└──  com
   └── wangguangwu
       └── distributedratelimiter
           ├── DistributedRateLimiterApplication.java
           ├── annotation
           │   └── DistributedRateLimiter.java
           ├── aspect
           │   └── DistributedRateLimitAspect.java
           ├── config
           │   ├── RedisConfig.java
           │   └── WebMvcConfig.java
           ├── context
           │   ├── RequestContext.java
           │   └── ResponseContext.java
           ├── controller
           │   └── ApiController.java
           ├── enums
           │   └── LimitType.java
           ├── interceptor
           │   └── HttpInterceptor.java
           └── util
               └── IpAddressUtil.java
```

### 3.3.2 依赖及配置项

#### 3.3.2.1 引入依赖

在 pom.xml 中引入 redis、jedis 和 SpringBoot AOP 的依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>

<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

#### 3.3.2.2 配置项

在 application.properties 中添加 Redis 的限流配置：

```properties
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.database=0
```

#### 3.3.2.3 Lua 脚本

通常我们使用 Redis 事务时，并不是直接使用 Redis 自身提供的事务功能，而是使用 Lua 脚本。相比 Redis 事务，Lua 脚本的优点：

- **减少网络开销**：使用 Lua 脚本，无需向 Redis 发送多次请求，执行一次即可，减少网络传输
- **原子操作**：Redis 将整个 Lua 脚本作为一个命令执行，原子，无需担心并发
- **复用**：Lua 脚本一旦执行，会永久保存 Redis 中,，其他客户端可复用。

**limit.lua 脚本**：

```lua
-- 获取限流的键和限流大小
local key = KEYS[1]
local limit = tonumber(ARGV[1])
-- 获取过期时间
local expire_time = tonumber(ARGV[2])

-- 获取当前的请求数量，如果键不存在则为0
local current = tonumber(redis.call('get', key) or "0")

if current + 1 > limit then
    -- 如果当前请求数量加1超过限制大小，返回0表示请求被拒绝
    return 0
else
    -- 使用一个事务来保证原子性
    redis.call("INCRBY", key, "1")
    redis.call("EXPIRE", key, expire_time)
    -- 如果需要调整时间单位为 ms    -- redis.call("PEXPIRE", key, expire_time)    return 1
end
```

### 3.3.3 核心代码

**DistributedRateLimiter**：自定义注解实现限流。

```java
import com.wangguangwu.distributedratelimiter.enums.LimitType;
import java.lang.annotation.*;

/**
 - 自定义限流注解，用于控制方法或类的访问频率。
 - <p>
 - 通过 Redis 和 Lua 脚本实现分布式限流。
 - 可以自定义限流的 key、前缀、时间范围、访问频率以及限流维度。
 - </p>
 *
 - @author wangguangwu
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DistributedRateLimiter {

    /**
     - 定义限流的唯一标识 key。
     - <p>
     - 可以用来区分不同的方法或资源，避免冲突。
     *
     - @return key 用于限流的唯一标识
     */
    String key();

    /**
     - 定义 key 的前缀。
     - <p>
     - 前缀用于区分不同业务或模块的限流 key。
     - 默认值为 "limiter:"。
     *
     - @return prefix key 的前缀
     */
    String prefix() default "limiter:";

    /**
     - 限流的时间范围，默认为 1 秒。
     - <p>
     - 该值表示在指定的时间范围内允许的最大请求次数。
     - 如果需要修改时间单位，需要同步修改对应的 Lua 脚本。
     *
     - @return period 限流的时间范围
     */
    int period() default 1;

    /**
     - 允许的最大访问次数，默认为 3 次。
     - <p>
     - 该值表示在指定的时间范围内，允许的最大请求次数。
     *
     - @return count 允许的最大访问次数
     */
    int count() default 3;

    /**
     - 限流的维度。
     - <p>
     - 可以根据 IP 地址进行限流，也可以根据自定义行为进行限流。
     - 默认值为 {@link LimitType#CUSTOMER}。
     *
     - @return limitType 限流的维度
     */
    LimitType limitType() default LimitType.CUSTOMER;

}
```

**DistributedRateLimitAspect**：自定义切面处理对应逻辑。

```java
import com.wangguangwu.distributedratelimiter.annotation.DistributedRateLimiter;
import com.wangguangwu.distributedratelimiter.context.ResponseContext;
import com.wangguangwu.distributedratelimiter.enums.LimitType;
import com.wangguangwu.distributedratelimiter.util.IpAddressUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 - 自定义切面，处理分布式限流注解 {@link DistributedRateLimiter}。
 - 通过 Lua 脚本在 Redis 中实现分布式限流。
 *
 - @author wangguangwu
 */
@Aspect
@Component
@Slf4j
public class DistributedRateLimitAspect {

    private static final String LIMIT_LUA_PATH = "limit.lua";

    @Resource
    private RedisTemplate<String, Serializable> limitRedisTemplate;

    private DefaultRedisScript<Long> redisScript;

    /**
     - 初始化方法，在 Bean 创建时加载 Lua 脚本。
     */
    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LIMIT_LUA_PATH)));
    }

    /**
     - 定义切点，匹配使用 {@link DistributedRateLimiter} 注解的方法。
     *
     - @param distributedRateLimiter 限流注解
     */
    @Pointcut("@annotation(distributedRateLimiter)")
    public void pointcut(DistributedRateLimiter distributedRateLimiter) {
    }

    /**
     - 环绕通知，处理限流逻辑。
     *
     - @param joinPoint              切入点
     - @param distributedRateLimiter 限流注解
     - @return 方法执行结果或降级处理结果
     */
    @Around(value = "pointcut(distributedRateLimiter)", argNames = "joinPoint,distributedRateLimiter")
    public Object around(ProceedingJoinPoint joinPoint, DistributedRateLimiter distributedRateLimiter) {
        // 获取速率和时间要求
        int limitPeriod = distributedRateLimiter.period();
        int limitCount = distributedRateLimiter.count();

        // 生成 Redis 键，区分限流类型
        String key = getKey(distributedRateLimiter.key(), distributedRateLimiter.limitType());
        String redisKey = StringUtils.join(distributedRateLimiter.prefix(), key);

        List<String> keys = Collections.singletonList(redisKey);

        try {
            Long result = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);

            // 判断是否获得令牌
            if (Boolean.TRUE.equals(result != null && result == 1)) {
                log.info("获取令牌成功，请求执行");
                return joinPoint.proceed();
            } else {
                // 服务降级处理
                fallback();
                return null;
            }
        } catch (Throwable e) {
            log.error("限流发生异常，走降级处理: {}", e.getMessage(), e);
            fallback();
            return null;
        }
    }

    /**
     - 降级处理方法。
     - 在限流条件触发时，返回错误信息给客户端。
     */
    private void fallback() {
        HttpServletResponse response = ResponseContext.getResponse();
        if (response != null) {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                writer.println("服务出错，请稍后重试");
                writer.flush();
            } catch (IOException e) {
                log.error("服务降级: {}", e.getMessage(), e);
            }
        }
    }

    /**
     - 根据限流类型生成 Redis 键。
     *
     - @param customKey 自定义键
     - @param limitType 限流类型
     - @return 生成的 Redis 键
     */
    private String getKey(String customKey, LimitType limitType) {
        String key = switch (limitType) {
            case IP -> IpAddressUtil.getIpAddress();
            case CUSTOMER -> customKey;
        };
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("限流键不可为空");
        }
        return key;
    }
}
```

## 3.4 服务降级（fallback 方法）

目的：往 HttpServletResponse 流中写入数据。

## 方式一：**通过 RequestContextHolder 获取 HttpServletResponse**

```java
public void fullback() {
    HttpServletResponse response = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse();
    if (response != null) {
        response.setHeader("Content-type", "text/html;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            log.info("服务出错，请稍后重试");
            writer.println("服务出错，请稍后重试");
            writer.flush();
        } catch (IOException e) {
            log.error("服务降级: {}", e.getMessage(), e);
        }
    }
}
```

**优点：**

- 不需要将 HttpServletResponse 注入为成员变量，可以在任何需要的地方灵活地获取当前请求的响应对象。
- 这种方式更加灵活，不需要依赖于 Spring 的自动注入机制，适合于需要动态获取 HttpServletResponse 的场景。

**缺点：**

- 依赖于 RequestContextHolder 和 Web 环境，如果在非 Web 环境中使用此代码，会抛出异常。
- 代码相对复杂且显得冗长。

## 方式二：Spring 注入 HttpServletResponse

```java
@Autowired
private HttpServletResponse response;

private void fallback() {
    response.setHeader("Content-Type", "text/html;charset=UTF8");
    try (PrintWriter writer = response.getWriter()) {
        writer.println("服务出错，请稍后重试");
        writer.flush();
    } catch (Exception e) {
        log.error("服务降级: {}", e.getMessage(), e);
    }
}
```

**优点：**

- 代码更加简洁，直接使用注入的 HttpServletResponse。
- 更加依赖于 Spring 的依赖注入机制，减少了样板代码。

**缺点：**

- HttpServletResponse 是线程不安全的，直接注入可能导致线程安全问题，尤其在并发环境下。

## 方式三：使用 ThreadLocal 来管理 HttpServletResponse

ThreadLocal 可以用来优化对 HttpServletResponse 的访问，以确保每个线程都有独立的 HttpServletResponse 实例。这可以减少直接依赖 RequestContextHolder 的样板代码，并提供一种更简洁的方式来管理每个线程的响应对象。

**ResponseContext**：使用 ThreadLocal 管理 HttpServletResponse

```java
import jakarta.servlet.http.HttpServletResponse;

/**
 - 使用 ThreadLocal 来管理 HttpServletResponse，以确保每个线程都能独立访问和管理自己的 HttpServletResponse 对象。
 - <p>
 - 该类提供了设置、获取和移除 HttpServletResponse 对象的静态方法。它常用于在多线程环境中需要安全地共享响应对象的场景，
 - 比如在过滤器、拦截器中设置响应对象，以便后续的业务逻辑能够访问和操作响应对象。
 - </p>
 *
 - @author wangguangwu
 */
 public class ResponseContext {

    /**
     - 使用 ThreadLocal 存储每个线程独立的 HttpServletResponse 实例
     */
    private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<>();

    /**
     - 设置当前线程的 HttpServletResponse 对象。
     *
     - @param response 当前线程的 HttpServletResponse 对象
     */
    public static void setResponse(HttpServletResponse response) {
        RESPONSE_HOLDER.set(response);
    }

    /**
     - 获取当前线程的 HttpServletResponse 对象。
     *
     - @return 当前线程的 HttpServletResponse 对象，如果没有设置则返回 null
     */    public static HttpServletResponse getResponse() {
        return RESPONSE_HOLDER.get();
    }

    /**
     - 移除当前线程的 HttpServletResponse 对象。
     - <p>
     - 该方法通常在请求处理完成后调用，以避免内存泄漏。确保每个请求处理完后都清除与当前线程关联的响应对象。
     - </p>
     */
    public static void removeResponse() {
        RESPONSE_HOLDER.remove();
    }
}
```

**ResponseInterceptor**：Http 请求拦截器

```java
import com.wangguangwu.guavaratelimiter.context.ResponseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 - Http 请求拦截器，用于在请求开始时绑定 HttpServletResponse 到 ThreadLocal，并在请求结束时清理。
 *
 - @author wangguangwu
 - @see ResponseContext
 - @see org.springframework.web.servlet.HandlerInterceptor
 */
@Component
public class ResponseInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
	    // 将 response 绑定到 ThreadLocal
        ResponseContext.setResponse(response);
        // 继续处理请求
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		// 清理 ThreadLocal 中的 response
	    ResponseContext.removeResponse();
    }
}
```

**WebMvcConfig**：WebMvc 配置类

```java
import com.wangguangwu.guavaratelimiter.interceptor.ResponseInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 - WebMvc 配置类，用于注册拦截器。
 *
 - @author wangguangwu
 - @see ResponseInterceptor
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器
        registry.addInterceptor(new ResponseInterceptor());
    }
}
```

**优点**：

- **简化代码**：不需要每次都从 RequestContextHolder 获取 HttpServletResponse。
- **线程安全**：每个请求都有自己独立的 HttpServletResponse，避免了线程安全问题。
- **集中管理**：通过 ThreadLocal 集中管理响应对象，方便在应用的任何地方访问。

**缺点**：

- **潜在的内存泄漏**：ThreadLocal 的值在使用后需要手动清除，否则可能会导致内存泄漏。