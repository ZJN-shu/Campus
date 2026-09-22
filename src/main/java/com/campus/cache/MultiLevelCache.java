package com.campus.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多级缓存服务（本地缓存 + Redis）
 */
@Slf4j
@Component
public class MultiLevelCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存（Caffeine）
     * - maximumSize: 最大1万条
     * - expireAfterWrite: 写入后5分钟过期
     * - recordStats: 统计命中率
     */
    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /**
     * 获取缓存
     * @param key 缓存键
     * @param loader 数据加载器（缓存未命中时调用）
     * @param ttl Redis过期时间（分钟）
     */
    public String get(String key, Function<String, String> loader, long ttl) {
        // 1. 查本地缓存
        String value = localCache.getIfPresent(key);
        if (value != null) {
            log.debug("本地缓存命中: {}", key);
            return value;
        }

        // 2. 查Redis
        value = stringRedisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("Redis缓存命中: {}", key);
            // 回填本地缓存
            localCache.put(key, value);
            return value;
        }

        // 3. 查数据库（通过loader）
        log.debug("缓存未命中，查询数据库: {}", key);
        value = loader.apply(key);

        if (value != null) {
            // 4. 写入Redis
            stringRedisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MINUTES);
            // 5. 写入本地缓存
            localCache.put(key, value);
        }

        return value;
    }

    /**
     * 获取缓存（默认30分钟过期）
     */
    public String get(String key, Function<String, String> loader) {
        return get(key, loader, 30);
    }

    /**
     * 删除缓存（同时删除本地和Redis）
     */
    public void evict(String key) {
        localCache.invalidate(key);
        stringRedisTemplate.delete(key);
        log.debug("清除缓存: {}", key);
    }

    /**
     * 批量删除
     */
    public void evictBatch(java.util.Collection<String> keys) {
        localCache.invalidateAll(keys);
        stringRedisTemplate.delete(keys);
        log.debug("批量清除缓存: {}", keys.size());
    }

    /**
     * 获取本地缓存命中率
     */
    public double getLocalHitRate() {
        return localCache.stats().hitRate();
    }

    /**
     * 获取本地缓存统计信息
     */
    public String getLocalStats() {
        return String.format("命中率: %.2f%%, 命中次数: %d, 未命中次数: %d",
                localCache.stats().hitRate() * 100,
                localCache.stats().hitCount(),
                localCache.stats().missCount());
    }
}