package com.campus.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEvent {

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 发生时间
     */
    private Long timestamp;

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 用户ID
     */
    private Long userId;

    public BaseEvent() {
        this.timestamp = System.currentTimeMillis();
    }
}