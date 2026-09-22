package com.campus.service;

import com.campus.dto.Result;
import com.campus.dto.StudentDTO;
import com.campus.entity.Order;
import com.campus.entity.Product;
import com.campus.mapper.ProductMapper;
import com.campus.service.Impl.StockCacheService;
import com.campus.utils.StudentHolder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockCacheConsistencyTest {

    @Autowired
    private IProductService productService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private StockCacheService stockCacheService;

    private static Long testProductId;
    private static final int INITIAL_STOCK = 100;

    private static Long testUserId = 1L;  // 使用已存在的用户ID
//    @Test
//    void testRedisConnection() {
//        String testKey = "test:connection";
//        stringRedisTemplate.opsForValue().set(testKey, "hello");
//        String value = stringRedisTemplate.opsForValue().get(testKey);
//        System.out.println("Redis value: " + value);
//        Assertions.assertNotNull(value);
//    }
    @BeforeAll
    static void setup() {
        log.info("========== 开始缓存一致性和预扣库存测试 ==========");

        // 模拟登录用户
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setId(testUserId);
        studentDTO.setNickName("测试用户");
        StudentHolder.saveStudent(studentDTO);

        log.info("模拟登录用户: id={}", testUserId);
    }

    @AfterAll
    static void tearDown() {
        StudentHolder.removeStudent();
        log.info("========== 测试完成 ==========");
    }
    // ==================== 测试1：商品发布初始化库存 ====================

    @Test
    @org.junit.jupiter.api.Order(1)
    void testPublishProductWithStock() {
        log.info("测试1：发布商品并初始化库存");

        Product product = new Product();
        product.setTitle("测试商品-库存测试");
        product.setDescription("这是一个用于库存测试的商品");
        product.setPrice(new java.math.BigDecimal("99.00"));
        product.setCategory("测试");
        product.setStock(INITIAL_STOCK);
        product.setReservedStock(0);
        product.setVersion(0);

        Result result = productService.publishProduct(product);
        assertTrue(result.getSuccess(), "商品发布失败");

        testProductId = (Long) result.getData();
        log.info("商品发布成功，ID: {}, 初始库存: {}", testProductId, INITIAL_STOCK);

        // 验证数据库中的库存
        Product savedProduct = productMapper.selectById(testProductId);
        assertNotNull(savedProduct, "商品不存在");
        assertEquals(INITIAL_STOCK, savedProduct.getStock(), "初始库存不正确");
        assertEquals(0, savedProduct.getReservedStock(), "预扣库存应为0");
    }

    // ==================== 测试2：缓存读取库存 ====================

    @Test
    @org.junit.jupiter.api.Order(2)
    void testCacheReadStock() {
        log.info("测试2：缓存读取库存");

        assertNotNull(testProductId, "请先运行测试1");

        // 第一次查询（缓存未命中，查数据库）
        long start1 = System.currentTimeMillis();
        Result result1 = productService.queryProductById(testProductId);
        long cost1 = System.currentTimeMillis() - start1;
        assertTrue(result1.getSuccess(), "查询商品失败");

        // 第二次查询（缓存命中）
        long start2 = System.currentTimeMillis();
        Result result2 = productService.queryProductById(testProductId);
        long cost2 = System.currentTimeMillis() - start2;
        assertTrue(result2.getSuccess(), "查询商品失败");

        log.info("第一次查询耗时: {}ms (缓存未命中)", cost1);
        log.info("第二次查询耗时: {}ms (缓存命中)", cost2);
        assertTrue(cost2 < cost1, "缓存应该提升性能");

        // 验证库存缓存存在
        String stockKey = "cache:stock:" + testProductId;
        String cachedStock = stringRedisTemplate.opsForValue().get(stockKey);
        assertNotNull(cachedStock, "库存缓存应该存在");
        assertEquals(String.valueOf(INITIAL_STOCK), cachedStock, "缓存库存不正确");
        log.info("库存缓存值: {}", cachedStock);
    }

    // ==================== 测试3：下单预扣库存 ====================

    @Test
    @org.junit.jupiter.api.Order(3)
    void testReserveStock() throws InterruptedException {
        log.info("测试3：下单预扣库存");

        assertNotNull(testProductId, "请先运行测试1");

        int quantity = 1;
        Result result = orderService.createOrder(testProductId, quantity);
        assertTrue(result.getSuccess(), "下单失败");

        Order order = (Order) result.getData();
        assertNotNull(order, "订单创建失败");
        log.info("订单创建成功: orderNo={}, 购买数量={}", order.getOrderNo(), quantity);

        // 等待缓存更新
        Thread.sleep(500);

        // 验证数据库预扣库存
        Product product = productMapper.selectById(testProductId);
        assertEquals(INITIAL_STOCK, product.getStock(), "实际库存不应变化");
        assertEquals(quantity, product.getReservedStock(), "预扣库存应增加");

        // 验证缓存：由于采用删除策略，缓存可能不存在，需要重新获取
        String stockKey = "cache:stock:" + testProductId;
        String cachedStock = stringRedisTemplate.opsForValue().get(stockKey);

        // 如果缓存不存在，手动触发加载
        if (cachedStock == null) {
            Integer reloaded = stockCacheService.getAvailableStock(testProductId);
            cachedStock = String.valueOf(reloaded);
            log.info("缓存不存在，重新加载后: {}", cachedStock);
        }

        int expectedStock = INITIAL_STOCK - quantity;
        assertEquals(String.valueOf(expectedStock), cachedStock, "缓存库存应减少");

        log.info("预扣库存后: 实际库存={}, 预扣库存={}, 缓存库存={}",
                product.getStock(), product.getReservedStock(), cachedStock);
    }

    // ==================== 测试4：多次下单预扣库存 ====================
    @Test
    @org.junit.jupiter.api.Order(4)
    void testMultipleReserveStock() throws InterruptedException {
        log.info("测试4：多次下单预扣库存");

        assertNotNull(testProductId, "请先运行测试1");

        int quantity = 2;
        Result result = orderService.createOrder(testProductId, quantity);
        assertTrue(result.getSuccess(), "第二次下单失败");

        Order order = (Order) result.getData();
        log.info("第二个订单创建成功: orderNo={}, 购买数量={}", order.getOrderNo(), quantity);

        Thread.sleep(500);

        Product product = productMapper.selectById(testProductId);
        int expectedReserved = 1 + quantity;
        assertEquals(expectedReserved, product.getReservedStock(), "预扣库存应累计增加");

        // 验证缓存：手动触发加载
        String stockKey = "cache:stock:" + testProductId;
        String cachedStock = stringRedisTemplate.opsForValue().get(stockKey);

        if (cachedStock == null) {
            Integer reloaded = stockCacheService.getAvailableStock(testProductId);
            cachedStock = String.valueOf(reloaded);
        }

        int expectedCacheStock = INITIAL_STOCK - expectedReserved;
        assertEquals(String.valueOf(expectedCacheStock), cachedStock, "缓存库存应正确减少");

        log.info("两次预扣后: 预扣库存={}, 缓存库存={}", product.getReservedStock(), cachedStock);
    }

    // ==================== 测试5：并发下单测试 ====================

    @Test
    @org.junit.jupiter.api.Order(5)
    void testConcurrentReserveStock() throws InterruptedException {
        log.info("测试5：并发下单测试（10个线程同时下单）");

        assertNotNull(testProductId, "请先运行测试1");

        // 获取当前库存
        Product beforeProduct = productMapper.selectById(testProductId);
        int beforeReserved = beforeProduct.getReservedStock();
        int beforeStock = beforeProduct.getStock();
        log.info("当前状态: 实际库存={}, 预扣库存={}", beforeStock, beforeReserved);

        int threadCount = 10;
        int quantityPerThread = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 保存当前用户信息到变量，供子线程使用
        StudentDTO currentStudent = StudentHolder.getStudent();
        assertNotNull(currentStudent, "当前用户未登录");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 在子线程中设置用户信息
                    StudentHolder.saveStudent(currentStudent);

                    Result result = orderService.createOrder(testProductId, quantityPerThread);
                    if (result.getSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        log.info("下单失败: {}", result.getErrorMsg());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("下单异常", e);
                } finally {
                    // 清理线程局部变量
                    StudentHolder.removeStudent();
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        long duration = System.currentTimeMillis() - startTime;

        // 等待缓存更新
        Thread.sleep(1000);

        Product afterProduct = productMapper.selectById(testProductId);
        int expectedReserved = beforeReserved + successCount.get();

        log.info("并发测试结果:");
        log.info("  总请求: {}", threadCount);
        log.info("  成功: {}", successCount.get());
        log.info("  失败: {}", failCount.get());
        log.info("  耗时: {}ms", duration);
        log.info("  最终预扣库存: {}", afterProduct.getReservedStock());
        log.info("  预期预扣库存: {}", expectedReserved);

        assertEquals(expectedReserved, afterProduct.getReservedStock(), "预扣库存应与成功下单数匹配");
        assertTrue(afterProduct.getReservedStock() <= afterProduct.getStock(), "预扣库存不应超过实际库存");
    }

    // ==================== 测试6：支付确认扣减库存 ====================

    @Test
    @org.junit.jupiter.api.Order(6)
    void testConfirmStock() throws InterruptedException {
        log.info("测试6：支付确认扣减库存");

        assertNotNull(testProductId, "请先运行测试1");

        // 先创建一个订单
        Result createResult = orderService.createOrder(testProductId, 1);
        assertTrue(createResult.getSuccess(), "创建订单失败");
        Order order = (Order) createResult.getData();
        log.info("创建订单: orderNo={}", order.getOrderNo());

        Thread.sleep(500);

        Product beforeProduct = productMapper.selectById(testProductId);
        int beforeStock = beforeProduct.getStock();
        int beforeReserved = beforeProduct.getReservedStock();
        log.info("支付前: 实际库存={}, 预扣库存={}", beforeStock, beforeReserved);

        // 支付订单
        Result payResult = orderService.payOrder(order.getId());
        assertTrue(payResult.getSuccess(), "支付失败");

        Thread.sleep(500);

        Product afterProduct = productMapper.selectById(testProductId);
        log.info("支付后: 实际库存={}, 预扣库存={}", afterProduct.getStock(), afterProduct.getReservedStock());

        // 验证：实际库存减少，预扣库存减少
        assertEquals(beforeStock - 1, afterProduct.getStock(), "实际库存应减少");
        assertEquals(beforeReserved - 1, afterProduct.getReservedStock(), "预扣库存应减少");

        // 验证缓存已更新或删除
        String stockKey = "cache:stock:" + testProductId;
        String cachedStock = stringRedisTemplate.opsForValue().get(stockKey);
        if (cachedStock != null) {
            int expectedCache = afterProduct.getStock() - afterProduct.getReservedStock();
            assertEquals(String.valueOf(expectedCache), cachedStock, "缓存库存应等于可用库存");
        }
    }

    // ==================== 测试7：取消订单释放库存 ====================

    @Test
    @org.junit.jupiter.api.Order(7)
    void testReleaseStock() throws InterruptedException {
        log.info("测试7：取消订单释放库存");
        String stockKey = "cache:stock:" + testProductId;
        assertNotNull(testProductId, "请先运行测试1");

        // 创建一个订单
        Result createResult = orderService.createOrder(testProductId, 1);
        assertTrue(createResult.getSuccess(), "创建订单失败");
        Order order = (Order) createResult.getData();
        log.info("创建订单: orderNo={}", order.getOrderNo());

        Thread.sleep(500);

        Product beforeProduct = productMapper.selectById(testProductId);
        int beforeReserved = beforeProduct.getReservedStock();
        log.info("取消前: 预扣库存={}", beforeReserved);

        // 取消订单
        Result cancelResult = orderService.cancelOrder(order.getId());
        assertTrue(cancelResult.getSuccess(), "取消订单失败");

        Thread.sleep(500);

        Product afterProduct = productMapper.selectById(testProductId);
        log.info("取消后: 预扣库存={}", afterProduct.getReservedStock());

        // 验证：预扣库存减少
        assertEquals(beforeReserved - 1, afterProduct.getReservedStock(), "预扣库存应减少");

        // 主动获取缓存库存（会触发重建）
        Integer cacheStock = stockCacheService.getAvailableStock(testProductId);
        int expectedCache = afterProduct.getStock() - afterProduct.getReservedStock();

        log.info("预期缓存库存: {}", expectedCache);
        log.info("实际缓存库存: {}", cacheStock);

        assertEquals(expectedCache, cacheStock, "缓存库存应正确恢复");
    }

    // ==================== 测试8：缓存与数据库一致性验证 ====================

    @Test
    @org.junit.jupiter.api.Order(8)
    void testCacheConsistency() {
        log.info("测试8：缓存与数据库一致性验证");

        assertNotNull(testProductId, "请先运行测试1");

        // 获取数据库实际库存
        Product product = productMapper.selectById(testProductId);
        int dbAvailableStock = product.getStock() - product.getReservedStock();

        // 获取缓存库存（如果不存在，重新加载）
        Integer cacheAvailableStock = stockCacheService.getAvailableStock(testProductId);

        log.info("数据库可用库存: {}", dbAvailableStock);
        log.info("缓存库存: {}", cacheAvailableStock);

        assertEquals(dbAvailableStock, cacheAvailableStock, "缓存应与数据库一致");
        log.info("✅ 缓存与数据库一致");
    }

    // ==================== 测试9：库存不足拒绝下单 ====================

    @Test
    @org.junit.jupiter.api.Order(9)
    void testInsufficientStock() {
        log.info("测试9：库存不足拒绝下单");

        assertNotNull(testProductId, "请先运行测试1");

        // 获取当前可用库存
        Product product = productMapper.selectById(testProductId);
        int availableStock = product.getStock() - product.getReservedStock();
        log.info("当前可用库存: {}", availableStock);

        // 尝试购买超过可用库存的数量
        int exceedQuantity = availableStock + 10;
        Result result = orderService.createOrder(testProductId, exceedQuantity);
        assertFalse(result.getSuccess(), "库存不足时应拒绝下单");
        assertTrue(result.getErrorMsg().contains("库存不足"), "错误信息应提示库存不足");

        log.info("库存不足拒绝下单: {}", result.getErrorMsg());
    }

    // ==================== 测试10：清理测试数据 ====================

    @Test
    @org.junit.jupiter.api.Order(10)
    void testCleanup() {
        log.info("测试10：清理测试数据");

        if (testProductId != null) {
            // 删除商品
            productService.deleteProduct(testProductId);

            // 清理缓存
            stringRedisTemplate.delete("cache:product:" + testProductId);
            stringRedisTemplate.delete("cache:stock:" + testProductId);

            log.info("测试商品已删除: {}", testProductId);
        }

        log.info("测试数据清理完成");
    }
}