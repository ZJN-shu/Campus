package com.campus.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ViewEvent extends BaseEvent {

    public ViewEvent() {
        this.setEventType("view");
    }

    public ViewEvent(Long targetId, Long userId, String targetType) {
        this();
        this.setTargetId(targetId);
        this.setUserId(userId);
        this.setTargetType(targetType);
    }
}