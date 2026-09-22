package com.campus.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LikeEvent extends BaseEvent {

    /**
     * 点赞类型：add / remove
     */
    private String action;

    public LikeEvent() {
        this.setEventType("like");
    }

    public LikeEvent(Long targetId, Long userId, String targetType, String action) {
        this();
        this.setTargetId(targetId);
        this.setUserId(userId);
        this.setTargetType(targetType);
        this.action = action;
    }
}