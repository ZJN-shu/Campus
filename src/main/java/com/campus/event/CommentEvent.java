package com.campus.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentEvent extends BaseEvent {

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 评论内容
     */
    private String content;

    public CommentEvent() {
        this.setEventType("comment");
    }

    public CommentEvent(Long targetId, Long userId, String targetType, Long commentId) {
        this();
        this.setTargetId(targetId);
        this.setUserId(userId);
        this.setTargetType(targetType);
        this.commentId = commentId;
    }
}