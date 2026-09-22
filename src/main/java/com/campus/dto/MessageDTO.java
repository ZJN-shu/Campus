package com.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageDTO {

    private Long id;
    private String content;
    private Integer type;
    private String targetType;
    private Long targetId;
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 发送者信息
    private Long fromUserId;
    private String fromUserName;
    private String fromUserAvatar;

    // 目标预览
    private String targetTitle;
    private String targetPreview;

    // 消息类型对应的图标和文案
    public String getTypeIcon() {
        switch (type) {
            case 1: return "💬"; // 评论
            case 2: return "👍"; // 点赞
            case 3: return "⭐"; // 收藏
            case 4: return "👥"; // 关注
            case 5: return "📢"; // 系统
            default: return "📝";
        }
    }

    public String getTypeText() {
        switch (type) {
            case 1: return "评论了你的";
            case 2: return "点赞了你的";
            case 3: return "收藏了你的";
            case 4: return "关注了你";
            case 5: return "系统通知";
            default: return "消息";
        }
    }
}