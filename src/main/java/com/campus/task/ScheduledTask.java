package com.campus.task;

import com.campus.processor.AsyncEventProcessor;
import com.campus.service.BatchUpdateService;
import com.campus.service.IHotRankService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class ScheduledTask {

    @Resource
    private BatchUpdateService batchUpdateService;

    @Resource
    private AsyncEventProcessor asyncEventProcessor;

    @Resource
    private IHotRankService hotRankService;

    /**
     * 每10秒批量刷新计数到数据库
     */
    @Scheduled(fixedDelay = 10000)
    public void flushCounts() {
        batchUpdateService.scheduledFlush();
    }

    /**
     * 每5分钟处理pending消息（重试）
     */
    @Scheduled(fixedDelay = 300000)
    public void processPendingMessages() {
        asyncEventProcessor.processPendingMessages();
    }


    @Scheduled(fixedDelay = 300000)
    public void recalculateHotScores() {
        hotRankService.recalculateHotScores();
    }
}