package com.campus.processor;

import com.alibaba.fastjson.JSON;
import com.campus.service.BatchUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ReliableEventConsumer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BatchUpdateService batchUpdateService;

    private static final String STREAM_LIKE = "stream:like";
    private static final String STREAM_VIEW = "stream:view";
    private static final String GROUP_NAME = "campus-group";
    private static final String CONSUMER_NAME = "consumer-" + System.currentTimeMillis();

    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        // 创建消费者组
        createConsumerGroup(STREAM_LIKE);
        createConsumerGroup(STREAM_VIEW);

        // 启动消费者
        startConsumer(STREAM_LIKE, this::processLikeEvent);
        startConsumer(STREAM_VIEW, this::processViewEvent);

        // 启动 Pending 消息恢复线程
        startPendingRecovery(STREAM_LIKE);
        startPendingRecovery(STREAM_VIEW);
    }

    /**
     * 创建消费者组
     */
    private void createConsumerGroup(String streamKey) {
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP_NAME);
            log.info("创建消费者组成功: {}", streamKey);
        } catch (Exception e) {
            log.debug("消费者组已存在: {}", streamKey);
        }
    }

    /**
     * 启动消费者
     */
    private void startConsumer(String streamKey, java.util.function.Consumer<MapRecord<String, Object, Object>> processor) {
        executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            while (running.get()) {
                try {
                    List<MapRecord<String, Object, Object>> messages =
                            stringRedisTemplate.opsForStream().read(
                                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                                    StreamReadOptions.empty()
                                            .count(10)
                                            .block(Duration.ofSeconds(2)),
                                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                            );

                    if (messages == null || messages.isEmpty()) {
                        continue;
                    }

                    for (MapRecord<String, Object, Object> message : messages) {
                        try {
                            // 处理消息
                            processor.accept(message);

                            // 关键：处理成功后发送 ACK 确认
                            stringRedisTemplate.opsForStream()
                                    .acknowledge(streamKey, GROUP_NAME, message.getId());

                            log.debug("消息处理成功并确认: stream={}, id={}", streamKey, message.getId());

                        } catch (Exception e) {
                            log.error("消息处理失败: stream={}, id={}", streamKey, message.getId(), e);
                            // 不发送 ACK，消息会留在 Pending List
                        }
                    }

                } catch (Exception e) {
                    log.error("消费消息异常", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }

    /**
     * 处理点赞事件
     */
    private void processLikeEvent(MapRecord<String, Object, Object> message) {
        Map<Object, Object> value = message.getValue();
        Long targetId = Long.valueOf(value.get("targetId").toString());
        Long userId = Long.valueOf(value.get("userId").toString());
        String action = value.get("action").toString();

        int delta = "add".equals(action) ? 1 : -1;
        batchUpdateService.addLikeCount("post", targetId, delta);

        log.debug("处理点赞事件: targetId={}, userId={}, action={}", targetId, userId, action);
    }

    /**
     * 处理浏览事件
     */
    private void processViewEvent(MapRecord<String, Object, Object> message) {
        Map<Object, Object> value = message.getValue();
        Long targetId = Long.valueOf(value.get("targetId").toString());
        Long userId = Long.valueOf(value.get("userId").toString());
        String targetType = value.get("targetType").toString();

        batchUpdateService.addViewCount(targetType, targetId, 1);

        log.debug("处理浏览事件: targetId={}, userId={}", targetId, userId);
    }

    /**
     * 启动 Pending 消息恢复
     */
    private void startPendingRecovery(String streamKey) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                recoverPendingMessages(streamKey);
            } catch (Exception e) {
                log.error("恢复 pending 消息失败: stream={}", streamKey, e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 恢复未确认的消息
     */
    /**
     * 恢复未确认的消息
     */
    private void recoverPendingMessages(String streamKey) {
        // 1. 查看 pending 消息统计
        PendingMessagesSummary summary = stringRedisTemplate.opsForStream()
                .pending(streamKey, GROUP_NAME);

        if (summary == null || summary.getTotalPendingMessages() == 0) {
            return;
        }

        log.info("发现 {} 条 pending 消息: stream={}", summary.getTotalPendingMessages(), streamKey);

        // 2. 获取 pending 消息详情
        PendingMessages pendingMessages = stringRedisTemplate.opsForStream()
                .pending(streamKey, GROUP_NAME, Range.unbounded(), 100);

        for (PendingMessage msg : pendingMessages) {
            // 检查消息是否超时（超过5分钟未确认）
            // getElapsedTimeSinceLastDelivery() 返回 Duration，需要转换为毫秒
            long idleTimeMillis = msg.getElapsedTimeSinceLastDelivery().toMillis();

            if (idleTimeMillis > 5 * 60 * 1000) {  // 5分钟 = 300000毫秒
                // 3. 转移消息到当前消费者重新处理
                List<MapRecord<String, Object, Object>> claimed =
                        stringRedisTemplate.opsForStream()
                                .claim(streamKey, GROUP_NAME, CONSUMER_NAME,
                                        Duration.ofSeconds(30), msg.getId());

                for (MapRecord<String, Object, Object> record : claimed) {
                    log.info("重新处理 pending 消息: stream={}, id={}", streamKey, record.getId());
                    try {
                        if (STREAM_LIKE.equals(streamKey)) {
                            processLikeEvent(record);
                        } else if (STREAM_VIEW.equals(streamKey)) {
                            processViewEvent(record);
                        }
                        stringRedisTemplate.opsForStream()
                                .acknowledge(streamKey, GROUP_NAME, record.getId());
                    } catch (Exception e) {
                        log.error("重新处理 pending 消息失败", e);
                    }
                }
            }
        }
    }
}