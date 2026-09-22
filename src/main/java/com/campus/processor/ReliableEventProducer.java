package com.campus.processor;

import com.alibaba.fastjson.JSON;
import com.campus.entity.DeadLetter;
import com.campus.mapper.DeadLetterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ReliableEventProducer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DeadLetterMapper deadLetterMapper;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 100;

    /**
     * 发送点赞事件（带重试）
     */
    public void sendLikeEvent(Long targetId, Long userId, String targetType, String action) {
        Map<String, String> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("targetId", String.valueOf(targetId));
        event.put("userId", String.valueOf(userId));
        event.put("targetType", targetType);
        event.put("action", action);
        event.put("timestamp", String.valueOf(System.currentTimeMillis()));

        sendWithRetry("stream:like", event);
    }

    /**
     * 发送浏览事件（带重试）
     */
    public void sendViewEvent(Long targetId, Long userId, String targetType) {
        Map<String, String> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("targetId", String.valueOf(targetId));
        event.put("userId", String.valueOf(userId));
        event.put("targetType", targetType);
        event.put("timestamp", String.valueOf(System.currentTimeMillis()));

        sendWithRetry("stream:view", event);
    }

    /**
     * 带重试机制的消息发送
     */
    private void sendWithRetry(String streamKey, Map<String, String> message) {
        Exception lastException = null;

        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                String messageId = String.valueOf(stringRedisTemplate.opsForStream().add(streamKey, message));
                log.debug("消息发送成功: stream={}, id={}", streamKey, messageId);
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("消息发送失败，第{}次重试: {}", retry + 1, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS * (retry + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 所有重试都失败，存入死信队列
        saveToDeadLetter(streamKey, message, lastException);
        log.error("消息发送最终失败，已存入死信队列: stream={}", streamKey);
    }

    /**
     * 存入死信队列
     */
    private void saveToDeadLetter(String streamKey, Map<String, String> message, Exception e) {
        try {
            DeadLetter deadLetter = new DeadLetter();
            deadLetter.setStreamKey(streamKey);
            deadLetter.setMessage(JSON.toJSONString(message));
            deadLetter.setErrorMsg(e.getMessage());
            deadLetter.setRetryCount(0);
            deadLetter.setStatus(0);
            deadLetterMapper.insert(deadLetter);
        } catch (Exception ex) {
            log.error("存入死信队列失败", ex);
        }
    }
}