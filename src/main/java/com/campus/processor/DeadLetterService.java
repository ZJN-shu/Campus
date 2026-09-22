package com.campus.processor;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.entity.DeadLetter;
import com.campus.mapper.DeadLetterMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeadLetterService {

    @Resource
    private DeadLetterMapper deadLetterMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final int MAX_RETRY = 3;

    /**
     * 定时处理死信队列（每5分钟执行一次）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void processDeadLetters() {
        List<DeadLetter> deadLetters = deadLetterMapper.selectList(
                new LambdaQueryWrapper<DeadLetter>()
                        .eq(DeadLetter::getStatus, 0)
                        .lt(DeadLetter::getRetryCount, MAX_RETRY)
                        .orderByAsc(DeadLetter::getCreateTime)
                        .last("LIMIT 100")
        );

        for (DeadLetter deadLetter : deadLetters) {
            try {
                // 重新发送消息
                Map<String, String> message = JSON.parseObject(deadLetter.getMessage(), Map.class);
                String messageId = String.valueOf(stringRedisTemplate.opsForStream()
                        .add(deadLetter.getStreamKey(), message));

                if (messageId != null) {
                    // 发送成功，标记为已处理
                    deadLetter.setStatus(1);
                    deadLetterMapper.updateById(deadLetter);
                    log.info("死信重试成功: id={}", deadLetter.getId());
                }
            } catch (Exception e) {
                log.error("死信重试失败: id={}", deadLetter.getId(), e);
                deadLetter.setRetryCount(deadLetter.getRetryCount() + 1);
                if (deadLetter.getRetryCount() >= MAX_RETRY) {
                    deadLetter.setStatus(2);  // 最终失败
                }
                deadLetterMapper.updateById(deadLetter);
            }
        }
    }
}