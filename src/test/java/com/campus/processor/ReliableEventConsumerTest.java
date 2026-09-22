package com.campus.processor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@SpringBootTest
public class ReliableEventConsumerTest {

    @Autowired
    private ReliableEventProducer producer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 测试1：消息能被正常消费和确认
     */
    @Test
    void testNormalConsumeAndAck() throws InterruptedException {
        log.info("========== 测试1：消息正常消费和确认 ==========");

        // 发送测试消息
        producer.sendLikeEvent(200L, 1L, "post", "add");
        log.info("消息已发送");

        // 等待消费者处理
        Thread.sleep(3000);

        // 检查 Stream 中是否有未确认的消息
        try {
            var pending = stringRedisTemplate.opsForStream()
                    .pending("stream:like", "campus-group");
            if (pending != null) {
                log.info("Pending消息数: {}", pending.getTotalPendingMessages());
            }
        } catch (Exception e) {
            log.warn("检查Pending失败: {}", e.getMessage());
        }

        log.info("测试完成");
    }

    /**
     * 测试2：批量消息消费
     */
    @Test
    void testBatchConsume() throws InterruptedException {
        log.info("========== 测试2：批量消息消费 ==========");

        // 发送100条消息
        for (int i = 0; i < 100; i++) {
            producer.sendLikeEvent((long) i, 1L, "post", "add");
        }
        log.info("100条消息已发送");

        // 等待消费者处理
        Thread.sleep(5000);

        log.info("批量消费测试完成");
    }
}