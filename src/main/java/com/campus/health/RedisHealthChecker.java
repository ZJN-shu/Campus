package com.campus.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RedisHealthChecker {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private int consecutiveFailures = 0;
    private int consecutiveSuccesses = 0;

    private static final int FAILURE_THRESHOLD = 3;
    private static final int RECOVERY_THRESHOLD = 2;

    @Scheduled(fixedDelay = 5000)
    public void healthCheck() {
        boolean currentHealthy = false;

        try {
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection().ping();
            currentHealthy = "PONG".equals(pong);
        } catch (Exception e) {
            log.warn("Redis健康检查失败: {}", e.getMessage());
        }

        updateHealthStatus(currentHealthy);
    }

    private synchronized void updateHealthStatus(boolean currentHealthy) {
        if (currentHealthy) {
            consecutiveFailures = 0;
            consecutiveSuccesses++;
            if (!healthy.get() && consecutiveSuccesses >= RECOVERY_THRESHOLD) {
                healthy.set(true);
                log.info("Redis 已恢复健康");
            }
        } else {
            consecutiveSuccesses = 0;
            consecutiveFailures++;
            if (healthy.get() && consecutiveFailures >= FAILURE_THRESHOLD) {
                healthy.set(false);
                log.error("Redis 连续{}次失败，标记为不健康", consecutiveFailures);
            }
        }
    }

    public boolean isHealthy() {
        return healthy.get();
    }

    /**
     * 获取详细状态（用于监控）
     */
    public HealthStatus getHealthStatus() {
        HealthStatus status = new HealthStatus();
        status.setHealthy(healthy.get());
        status.setConsecutiveFailures(consecutiveFailures);
        return status;
    }

    // ========== 内部类 ==========

    @lombok.Data
    public static class HealthStatus {
        private boolean healthy;
        private int consecutiveFailures;
    }
}