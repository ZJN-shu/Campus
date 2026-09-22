package com.campus.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
public class FallbackServiceTest {

    @Autowired
    private FallbackService fallbackService;

    /**
     * 测试1：存入本地队列
     */
    @Test
    void testSaveToLocalQueue() {
        log.info("========== 测试1：存入本地队列 ==========");

        int beforeSize = fallbackService.getLocalQueueSize();
        log.info("存入前队列大小: {}", beforeSize);

        fallbackService.saveToLocalQueue(1L, 1L, "post", "add");
        fallbackService.saveToLocalQueue(2L, 1L, "post", "add");
        fallbackService.saveToLocalQueue(3L, 1L, "post", "add");

        int afterSize = fallbackService.getLocalQueueSize();
        log.info("存入后队列大小: {}", afterSize);

        assertEquals(beforeSize + 3, afterSize);
    }

    /**
     * 测试2：批量存入本地队列
     */
    @Test
    void testBatchSaveToLocalQueue() {
        log.info("========== 测试2：批量存入本地队列 ==========");

        int batchSize = 50;
        for (int i = 0; i < batchSize; i++) {
            fallbackService.saveToLocalQueue((long) i, 1L, "post", "add");
        }

        int queueSize = fallbackService.getLocalQueueSize();
        log.info("批量存入{}条后队列大小: {}", batchSize, queueSize);

        assertTrue(queueSize >= batchSize);
    }
}