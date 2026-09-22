package com.campus.cache;

import com.campus.entity.Post;
import com.campus.service.IPostService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;

@Slf4j
@SpringBootTest
public class MultiLevelCacheTest {

    @Resource
    private MultiLevelCache multiLevelCache;

    @Resource
    private IPostService postService;

    @Test
    void testLocalCacheHitRate() {
        // 查看本地缓存命中率
        double hitRate = multiLevelCache.getLocalHitRate();
        log.info("本地缓存命中率: {}", hitRate);
    }

    @Test
    void testMultiLevelCachePerformance() {
        Long postId = 1L;

        // 第一次请求：缓存未命中
        long start = System.currentTimeMillis();
        postService.queryPostById(postId);
        long firstCost = System.currentTimeMillis() - start;

        // 第二次请求：本地缓存命中
        start = System.currentTimeMillis();
        postService.queryPostById(postId);
        long secondCost = System.currentTimeMillis() - start;

        log.info("第一次请求耗时: {}ms", firstCost);
        log.info("第二次请求耗时: {}ms", secondCost);
        log.info("性能提升: {}ms ({}%)",
                firstCost - secondCost,
                (firstCost - secondCost) * 100 / firstCost);
    }


    @Test
    void testMultipleRequests() {
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            postService.queryPostById(1L);
            long cost = System.currentTimeMillis() - start;
            log.info("第{}次请求耗时: {}ms", i + 1, cost);
        }
        log.info("最终命中率: {}", multiLevelCache.getLocalHitRate());
    }
}