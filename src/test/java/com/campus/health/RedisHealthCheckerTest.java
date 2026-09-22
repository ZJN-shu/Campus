package com.campus.health;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
public class RedisHealthCheckerTest {

    @Autowired
    private RedisHealthChecker healthChecker;

    /**
     * 测试1：获取健康状态
     */
    @Test
    void testGetHealthStatus() {
        log.info("========== 测试1：获取健康状态 ==========");

        boolean isHealthy = healthChecker.isHealthy();
        log.info("Redis健康状态: {}", isHealthy);

        // 健康状态可能是 true 或 false，取决于 Redis 是否可用
        // 这里只打印，不做断言
    }

    /**
     * 测试2：获取详细健康状态
     */
    @Test
    void testGetDetailedHealthStatus() {
        log.info("========== 测试2：获取详细健康状态 ==========");

        RedisHealthChecker.HealthStatus status = healthChecker.getHealthStatus();
        log.info("健康状态: {}", status.isHealthy());
        log.info("连续失败次数: {}", status.getConsecutiveFailures());

        assertNotNull(status);
    }

    /**
     * 测试3：健康检查不会阻塞
     */
    @Test
    void testHealthCheckNonBlocking() throws InterruptedException {
        log.info("========== 测试3：健康检查不阻塞 ==========");

        long startTime = System.currentTimeMillis();

        // 健康检查是异步定时执行的，这里只是获取状态
        boolean isHealthy = healthChecker.isHealthy();

        long duration = System.currentTimeMillis() - startTime;
        log.info("获取健康状态耗时: {}ms", duration);
        log.info("健康状态: {}", isHealthy);

        assertTrue(duration < 100, "获取健康状态应该在100ms内完成");
    }
}