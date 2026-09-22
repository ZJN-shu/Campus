package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_message")
public class Message {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发送者ID
     */
    private Long fromUserId;

    /**
     * 接收者ID
     */
    private Long toUserId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：1评论 2点赞 3收藏 4关注 5系统
     */
    private Integer type;

    /**
     * 关联类型：post/product/comment
     */
    private String targetType;

    /**
     * 关联ID
     */
    private Long targetId;

    /**
     * 是否已读：0未读 1已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 非数据库字段 ==========

    /**
     * 发送者昵称
     */
    @TableField(exist = false)
    private String fromUserName;

    /**
     * 发送者头像
     */
    @TableField(exist = false)
    private String fromUserAvatar;

    /**
     * 目标标题（帖子标题/商品标题）
     */
    @TableField(exist = false)
    private String targetTitle;
}