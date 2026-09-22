package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_hot_config")
public class HotConfig {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 目标类型 post/product
     */
    private String targetType;

    /**
     * 基础分
     */
    private Integer baseScore;

    /**
     * 浏览权重
     */
    private Float viewWeight;

    /**
     * 点赞权重
     */
    private Float likeWeight;

    /**
     * 评论权重
     */
    private Float commentWeight;

    /**
     * 分享权重
     */
    private Float shareWeight;

    /**
     * 收藏权重
     */
    private Float favoriteWeight;

    /**
     * 时间衰减因子
     */
    private Float timeDecay;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}