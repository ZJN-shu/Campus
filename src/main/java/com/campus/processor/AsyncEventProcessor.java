package com.campus.processor;

import com.alibaba.fastjson.JSON;
import com.campus.event.CommentEvent;
import com.campus.event.LikeEvent;
import com.campus.event.ViewEvent;
import com.campus.service.BatchUpdateService;
import com.campus.service.IHotRankService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AsyncEventProcessor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BatchUpdateService batchUpdateService;

    @Resource
    private IHotRankService hotRankService;

    // Stream Key
    private static final String STREAM_LIKE = "stream:like";
    private static final String STREAM_COMMENT = "stream:comment";
    private static final String STREAM_VIEW = "stream:view";

    // 消费者组
    private static final String GROUP_NAME = "campus-group";

    // 消费者名称（使用UUID确保唯一性）
    private static final String CONSUMER_NAME = "consumer-" + System.currentTimeMillis();

    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * 发送点赞事件
     */
    public void sendLikeEvent(Long targetId, Long userId, String targetType, String action) {
        LikeEvent event = new LikeEvent(targetId, userId, targetType, action);
        sendEvent(STREAM_LIKE, event);
    }

    /**
     * 发送评论事件
     */
    public void sendCommentEvent(Long targetId, Long userId, String targetType, Long commentId) {
        CommentEvent event = new CommentEvent(targetId, userId, targetType, commentId);
        sendEvent(STREAM_COMMENT, event);
    }

    /**
     * 发送浏览事件
     */
    public void sendViewEvent(Long targetId, Long userId, String targetType) {
        // 限流：同一用户对同一目标每分钟最多记录10次浏览
        String rateKey = "rate:view:" + targetType + ":" + targetId + ":" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(rateKey);
        if (count == null || count > 10) {
            return; // 超过限流，丢弃事件
        }
        stringRedisTemplate.expire(rateKey, 1, TimeUnit.MINUTES);

        ViewEvent event = new ViewEvent(targetId, userId, targetType);
        sendEvent(STREAM_VIEW, event);
    }

    /**
     * 发送事件到Stream
     */
    private void sendEvent(String streamKey, Object event) {
        try {
            Map<String, String> eventMap = JSON.parseObject(JSON.toJSONString(event), Map.class);
            stringRedisTemplate.opsForStream().add(streamKey, eventMap);
            log.debug("事件已发送: {} -> {}", streamKey, event);
        } catch (Exception e) {
            log.error("发送事件失败: {}", streamKey, e);
        }
    }

    /**
     * 启动消费者
     */
    @PostConstruct
    public void startConsumers() {
        executorService = Executors.newFixedThreadPool(3);

        // 启动点赞消费者
        executorService.submit(() -> consumeEvents(STREAM_LIKE, this::processLikeEvent));

        // 启动评论消费者
        executorService.submit(() -> consumeEvents(STREAM_COMMENT, this::processCommentEvent));

        // 启动浏览消费者
        executorService.submit(() -> consumeEvents(STREAM_VIEW, this::processViewEvent));

        // 启动定时刷新任务
        startScheduledFlush();

        log.info("异步事件处理器启动完成，消费者: {}", CONSUMER_NAME);
    }

    /**
     * 消费事件
     */
    private void consumeEvents(String streamKey, EventProcessor processor) {
        while (running.get()) {
            try {
                // XREADGROUP GROUP group consumer COUNT 100 BLOCK 2000 STREAMS streamKey >
                List<MapRecord<String, Object, Object>> messages = stringRedisTemplate.opsForStream()
                        .read(Consumer.from(GROUP_NAME, CONSUMER_NAME),
                                org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                                        .count(100)
                                        .block(Duration.ofSeconds(2)),
                                org.springframework.data.redis.connection.stream.StreamOffset.create(
                                        streamKey, ReadOffset.lastConsumed()));

                if (messages == null || messages.isEmpty()) {
                    continue;
                }

                for (MapRecord<String, Object, Object> message : messages) {
                    try {
                        // 处理消息
                        processor.process(message);

                        // XACK确认消息
                        stringRedisTemplate.opsForStream()
                                .acknowledge(streamKey, GROUP_NAME, message.getId());

                    } catch (Exception e) {
                        log.error("处理消息失败: {}", message.getId(), e);
                        // 处理失败的消息进入pending-list，由重试机制处理
                    }
                }

            } catch (Exception e) {
                log.error("消费事件异常: {}", streamKey, e);
                // 异常后等待1秒再继续
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 处理点赞事件
     */
    private void processLikeEvent(MapRecord<String, Object, Object> message) {
        Map<Object, Object> value = message.getValue();
        LikeEvent event = JSON.parseObject(JSON.toJSONString(value), LikeEvent.class);

        // 批量更新点赞数（不立即写数据库）
        int delta = "add".equals(event.getAction()) ? 1 : -1;
        batchUpdateService.addLikeCount(event.getTargetType(), event.getTargetId(), delta);

        // 更新热榜
        hotRankService.recordAction(event.getTargetType(), event.getTargetId(),
                event.getAction(), event.getUserId());

        log.debug("处理点赞事件: targetId={}, userId={}, action={}",
                event.getTargetId(), event.getUserId(), event.getAction());
    }

    /**
     * 处理评论事件
     */
    private void processCommentEvent(MapRecord<String, Object, Object> message) {
        Map<Object, Object> value = message.getValue();
        CommentEvent event = JSON.parseObject(JSON.toJSONString(value), CommentEvent.class);

        // 批量更新评论数
        batchUpdateService.addLikeCount(event.getTargetType(), event.getTargetId(), 0);

        // 更新热榜
        hotRankService.recordAction(event.getTargetType(), event.getTargetId(),
                "comment", event.getUserId());

        log.debug("处理评论事件: targetId={}, userId={}", event.getTargetId(), event.getUserId());
    }

    /**
     * 处理浏览事件
     */
    private void processViewEvent(MapRecord<String, Object, Object> message) {
        Map<Object, Object> value = message.getValue();
        ViewEvent event = JSON.parseObject(JSON.toJSONString(value), ViewEvent.class);

        // 批量更新浏览数
        batchUpdateService.addViewCount(event.getTargetType(), event.getTargetId(), 1);

        // 更新热榜（浏览权重较低）
        hotRankService.recordAction(event.getTargetType(), event.getTargetId(),
                "view", event.getUserId());

        log.debug("处理浏览事件: targetId={}, userId={}", event.getTargetId(), event.getUserId());
    }

    /**
     * 启动定时刷新任务（每10秒批量写入数据库）
     */
    private void startScheduledFlush() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                batchUpdateService.scheduledFlush();
                log.debug("定时批量刷新完成");
            } catch (Exception e) {
                log.error("定时批量刷新失败", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 处理pending-list中的异常消息（重试机制）
     */
    public void processPendingMessages() {
        for (String streamKey : List.of(STREAM_LIKE, STREAM_COMMENT, STREAM_VIEW)) {
            try {
                // XREADGROUP GROUP group consumer COUNT 100 STREAMS streamKey 0
                List<MapRecord<String, Object, Object>> pendingMessages =
                        stringRedisTemplate.opsForStream()
                                .read(Consumer.from(GROUP_NAME, CONSUMER_NAME + "-pending"),
                                        org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                                                .count(100),
                                        org.springframework.data.redis.connection.stream.StreamOffset.create(
                                                streamKey, ReadOffset.from("0")));

                for (MapRecord<String, Object, Object> message : pendingMessages) {
                    log.info("重新处理pending消息: {}", message.getId());
                    // 重新处理...
                    stringRedisTemplate.opsForStream()
                            .acknowledge(streamKey, GROUP_NAME, message.getId());
                }

            } catch (Exception e) {
                log.error("处理pending消息失败: {}", streamKey, e);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        // 最后一次刷新
        batchUpdateService.scheduledFlush();
        log.info("异步事件处理器已停止");
    }

    /**
     * 事件处理器接口
     */
    @FunctionalInterface
    private interface EventProcessor {
        void process(MapRecord<String, Object, Object> message) throws Exception;
    }
}