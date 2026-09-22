package com.campus.stress;

import com.campus.CampusApplication;
import com.campus.service.*;
import com.campus.task.OrderTimeoutScheduler;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CampusApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StressTest {

    @Autowired
    private ILikeService likeService;

    @Autowired
    private IHotRankService hotRankService;

    @Autowired
    private IProductService productService;

    @Autowired
    private ElasticsearchService esService;

    @Autowired
    private IPostService postService;

    @Autowired
    private OrderTimeoutScheduler orderTimeoutScheduler;

    private static final int WARM_UP_ITERATIONS = 100;
    private static final int STRESS_ITERATIONS = 1000;

    private final ExecutorService executor = Executors.newFixedThreadPool(50);

    @Test
    @Order(1)
    @DisplayName("1. 热榜查询性能压测")
    public void testHotRankQueryStress() throws InterruptedException {
        System.out.println("\n========== 热榜查询性能压测 ==========");

        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            hotRankService.getHotRank("all", 20);
        }

        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(STRESS_ITERATIONS);

        for (int i = 0; i < STRESS_ITERATIONS; i++) {
            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    hotRankService.getHotRank("all", 20);
                    successCount.incrementAndGet();
                    long latency = (System.nanoTime() - requestStart) / 1_000_000;
                    latencies.add(latency);
                    totalLatency.addAndGet(latency);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        latencies.sort(Long::compareTo);
        long p50 = latencies.get(latencies.size() / 2);
        long p90 = latencies.get((int) (latencies.size() * 0.9));
        long p99 = latencies.get((int) (latencies.size() * 0.99));

        double qps = (successCount.get() * 1000.0) / duration;
        double avgLatency = totalLatency.get() / (double) successCount.get();

        System.out.println("总请求数: " + STRESS_ITERATIONS);
        System.out.println("成功: " + successCount.get());
        System.out.println("失败: " + failCount.get());
        System.out.println("耗时: " + duration + "ms");
        System.out.println("QPS: " + String.format("%.2f", qps));
        System.out.println("平均延迟: " + String.format("%.2f", avgLatency) + "ms");
        System.out.println("P50延迟: " + p50 + "ms");
        System.out.println("P90延迟: " + p90 + "ms");
        System.out.println("P99延迟: " + p99 + "ms");

        assertTrue(successCount.get() > STRESS_ITERATIONS * 0.95, "成功率应大于95%");
        assertTrue(avgLatency < 100, "平均延迟应小于100ms");
    }

    @Test
    @Order(2)
    @DisplayName("2. 点赞功能并发压测")
    public void testLikeConcurrencyStress() throws InterruptedException {
        System.out.println("\n========== 点赞功能并发压测 ==========");

        int concurrentUsers = 100;
        int likesPerUser = 10;
        long postId = 1L;
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalFail = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(concurrentUsers);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                for (int j = 0; j < likesPerUser; j++) {
                    long requestStart = System.nanoTime();
                    try {
                        likeService.like("post", postId);
                        totalSuccess.incrementAndGet();
                        long latency = (System.nanoTime() - requestStart) / 1_000_000;
                        latencies.add(latency);
                        totalLatency.addAndGet(latency);
                    } catch (Exception e) {
                        totalFail.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        latencies.sort(Long::compareTo);
        long p50 = latencies.isEmpty() ? 0 : latencies.get(latencies.size() / 2);
        long p90 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.9));

        double qps = (totalSuccess.get() * 1000.0) / duration;
        double avgLatency = totalLatency.get() / (double) Math.max(totalSuccess.get(), 1);

        System.out.println("总请求数: " + (concurrentUsers * likesPerUser));
        System.out.println("成功: " + totalSuccess.get());
        System.out.println("失败: " + totalFail.get());
        System.out.println("QPS: " + String.format("%.2f", qps));
        System.out.println("平均延迟: " + String.format("%.2f", avgLatency) + "ms");
        System.out.println("P50延迟: " + p50 + "ms");
        System.out.println("P90延迟: " + p90 + "ms");

        assertTrue(totalSuccess.get() > 0, "至少应该有成功的点赞请求");
    }

    @Test
    @Order(3)
    @DisplayName("3. Elasticsearch搜索压测")
    public void testSearchStress() throws InterruptedException {
        System.out.println("\n========== Elasticsearch搜索压测 ==========");

        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            try {
                esService.searchProducts("商品", null, 1, 10);
            } catch (Exception ignored) {}
        }

        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(STRESS_ITERATIONS);

        String[] keywords = {"商品", "电脑", "手机", "书籍", "衣服"};
        for (int i = 0; i < STRESS_ITERATIONS; i++) {
            final String keyword = keywords[i % keywords.length];
            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    esService.searchProducts(keyword, null, 1, 10);
                    successCount.incrementAndGet();
                    long latency = (System.nanoTime() - requestStart) / 1_000_000;
                    latencies.add(latency);
                    totalLatency.addAndGet(latency);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        latencies.sort(Long::compareTo);
        long p50 = latencies.isEmpty() ? 0 : latencies.get(latencies.size() / 2);
        long p90 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.9));
        long p99 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.99));

        double qps = (successCount.get() * 1000.0) / duration;
        double avgLatency = totalLatency.get() / (double) Math.max(successCount.get(), 1);

        System.out.println("总请求数: " + STRESS_ITERATIONS);
        System.out.println("成功: " + successCount.get());
        System.out.println("失败: " + failCount.get());
        System.out.println("QPS: " + String.format("%.2f", qps));
        System.out.println("平均延迟: " + String.format("%.2f", avgLatency) + "ms");
        System.out.println("P50延迟: " + p50 + "ms");
        System.out.println("P90延迟: " + p90 + "ms");
        System.out.println("P99延迟: " + p99 + "ms");
    }

    @Test
    @Order(4)
    @DisplayName("4. 帖子查询压测")
    public void testPostQueryStress() throws InterruptedException {
        System.out.println("\n========== 帖子查询压测 ==========");

        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            postService.queryPostById(1L);
        }

        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        int totalRequests = STRESS_ITERATIONS;
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            final Long postId = (long) (i % 10 + 1);
            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    postService.queryPostById(postId);
                    successCount.incrementAndGet();
                    long latency = (System.nanoTime() - requestStart) / 1_000_000;
                    latencies.add(latency);
                    totalLatency.addAndGet(latency);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        latencies.sort(Long::compareTo);
        long p50 = latencies.get(latencies.size() / 2);
        long p90 = latencies.get((int) (latencies.size() * 0.9));
        long p99 = latencies.get((int) (latencies.size() * 0.99));

        double qps = (successCount.get() * 1000.0) / duration;
        double avgLatency = totalLatency.get() / (double) successCount.get();

        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功: " + successCount.get());
        System.out.println("失败: " + failCount.get());
        System.out.println("QPS: " + String.format("%.2f", qps));
        System.out.println("平均延迟: " + String.format("%.2f", avgLatency) + "ms");
        System.out.println("P50延迟: " + p50 + "ms");
        System.out.println("P90延迟: " + p90 + "ms");
        System.out.println("P99延迟: " + p99 + "ms");
    }

    @Test
    @Order(5)
    @DisplayName("5. 订单超时处理并发测试")
    public void testOrderTimeoutConcurrency() throws InterruptedException {
        System.out.println("\n========== 订单超时处理并发测试 ==========");

        int concurrentOrders = 50;
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentOrders; i++) {
            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    orderTimeoutScheduler.processTimeoutOrders();
                    successCount.incrementAndGet();
                    totalLatency.addAndGet((System.nanoTime() - requestStart) / 1_000_000);
                } catch (Exception e) {
                    System.err.println("订单超时处理异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        double avgLatency = totalLatency.get() / (double) Math.max(successCount.get(), 1);
        double qps = (successCount.get() * 1000.0) / duration;

        System.out.println("处理次数: " + successCount.get());
        System.out.println("耗时: " + duration + "ms");
        System.out.println("QPS: " + String.format("%.2f", qps));
        System.out.println("平均延迟: " + String.format("%.2f", avgLatency) + "ms");

        assertTrue(successCount.get() > 0, "应该有成功的处理");
    }

    @Test
    @Order(6)
    @DisplayName("6. 综合场景压测（点赞+热榜+搜索）")
    public void testComprehensiveStress() throws InterruptedException {
        System.out.println("\n========== 综合场景压测 ==========");

        int totalRequests = 500;
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger[] results = {
            new AtomicInteger(0),
            new AtomicInteger(0),
            new AtomicInteger(0)
        };
        AtomicLong[] latencies = {
            new AtomicLong(0),
            new AtomicLong(0),
            new AtomicLong(0)
        };

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int type = i % 3;
            executor.submit(() -> {
                long requestStart = System.nanoTime();
                try {
                    switch (type) {
                        case 0 -> hotRankService.getHotRank("all", 20);
                        case 1 -> likeService.like("post", 1L);
                        case 2 -> esService.searchProducts("商品", null, 1, 10);
                    }
                    results[type].incrementAndGet();
                    latencies[type].addAndGet((System.nanoTime() - requestStart) / 1_000_000);
                } catch (Exception ignored) {}
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        String[] names = {"热榜查询", "点赞操作", "搜索操作"};
        double totalQps = (totalRequests * 1000.0) / duration;

        System.out.println("\n各场景统计：");
        for (int i = 0; i < 3; i++) {
            double qps = (results[i].get() * 1000.0) / duration;
            double avgLat = latencies[i].get() / (double) Math.max(results[i].get(), 1);
            System.out.println(names[i] + " - 成功: " + results[i].get() +
                             ", QPS: " + String.format("%.2f", qps) +
                             ", 平均延迟: " + String.format("%.2f", avgLat) + "ms");
        }

        System.out.println("\n综合 QPS: " + String.format("%.2f", totalQps));
        System.out.println("总耗时: " + duration + "ms");

        assertTrue(totalQps > 5, "综合QPS应该大于5");
    }

    @AfterAll
    public void cleanup() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
