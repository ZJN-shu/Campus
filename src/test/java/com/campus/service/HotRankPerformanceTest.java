package com.campus.service;

import com.campus.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootTest
public class HotRankPerformanceTest {

    @Autowired
    private IHotRankService hotRankService;

    @Autowired
    private IPostService postService;

    @Test
    void testConcurrentRecordAction() throws InterruptedException {
        log.info("========== 并发记录热度测试 ==========");

        int threadCount = 10;
        int actionsPerThread = 100;
        AtomicInteger successCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(CompletableFuture.runAsync(() -> {
                for (int j = 0; j < actionsPerThread; j++) {
                    try {
                        long postId = (j % 5) + 1;
                        hotRankService.recordAction("post", postId, "view", (long) threadId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("记录失败", e);
                    }
                }
            }));
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("完成 {} 次操作，耗时 {}ms，QPS: {}",
                successCount.get(),
                duration,
                successCount.get() * 1000L / duration);

        // 等待异步处理完成
        Thread.sleep(3000);
    }

    @Test
    void testGetHotRankPerformance() {
        log.info("========== 热榜查询性能测试 ==========");

        int testCount = 100;
        List<Long> costs = new ArrayList<>();

        for (int i = 0; i < testCount; i++) {
            long start = System.nanoTime();
            Result result = hotRankService.getHotRank("all", 1);
            long end = System.nanoTime();
            costs.add(TimeUnit.NANOSECONDS.toMicros(end - start));
        }

        double avgCost = costs.stream().mapToLong(Long::longValue).average().orElse(0);
        long minCost = costs.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxCost = costs.stream().mapToLong(Long::longValue).max().orElse(0);

        log.info("热榜查询 {} 次", testCount);
        log.info("  平均耗时: {} μs", avgCost);
        log.info("  最小耗时: {} μs", minCost);
        log.info("  最大耗时: {} μs", maxCost);
        log.info("  QPS: {}", 1000000 / avgCost);
    }
}