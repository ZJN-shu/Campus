package com.campus.service;

import com.campus.processor.ReliableEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
public class FallbackService {

    @Resource
    private ReliableEventProducer reliableEventProducer;

    // 本地降级队列（Redis不可用时暂存）
    private final Queue<LikeEvent> localQueue = new ConcurrentLinkedQueue<>();

    /**
     * 保存到本地队列（降级时使用）
     */
    public void saveToLocalQueue(Long targetId, Long userId, String targetType, String action) {
        LikeEvent event = new LikeEvent(targetId, userId, targetType, action);
        localQueue.offer(event);
        log.info("点赞事件已存入本地队列，当前队列大小: {}", localQueue.size());
    }

    /**
     * 定时处理本地降级队列（每10秒执行一次）
     */
    @Scheduled(fixedDelay = 10000)
    public void processLocalQueue() {
        if (localQueue.isEmpty()) {
            return;
        }

        LikeEvent event;
        int processed = 0;
        int failed = 0;

        while ((event = localQueue.poll()) != null && processed < 100) {
            try {
                reliableEventProducer.sendLikeEvent(
                        event.getTargetId(),
                        event.getUserId(),
                        event.getTargetType(),
                        event.getAction()
                );
                processed++;
            } catch (Exception e) {
                log.error("处理本地队列失败，重新入队", e);
                localQueue.offer(event);
                failed++;
                // 避免无限重试，如果失败次数太多，记录日志后丢弃
                if (failed > 10) {
                    log.error("本地队列处理失败次数过多，丢弃消息: {}", event);
                }
                break;
            }
        }

        if (processed > 0) {
            log.info("处理本地队列: 成功{}条，失败{}条", processed, failed);
        }
    }

    /**
     * 获取队列大小（用于监控）
     */
    public int getLocalQueueSize() {
        return localQueue.size();
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LikeEvent {
        private Long targetId;
        private Long userId;
        private String targetType;
        private String action;
    }
}