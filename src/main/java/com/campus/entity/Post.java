package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_post")
public class Post {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    @NotBlank(message = "帖子标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度必须在2-100字之间")
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 50000, message = "内容长度不能超过50000字")
    private String content;

    private Integer type;

    @Size(max = 20, message = "板块名称长度不能超过20")
    private String category;

    @Size(max = 200, message = "标签长度不能超过200")
    private String tags;

    @Size(max = 2000, message = "图片链接长度不能超过2000")
    private String images;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer favoriteCount;
    private Integer isTop;
    private Integer isEssence;
    private Integer status;
    // 确保字段名匹配（注意下划线转驼峰）
    @TableField("hot_score")
    private BigDecimal hotScore;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 辅助方法
    public java.util.List<String> getImageList() {
        if (this.images == null || this.images.isEmpty() || "[]".equals(this.images)) {
            return new java.util.ArrayList<>();
        }
        return com.alibaba.fastjson.JSON.parseArray(this.images, String.class);
    }

    public java.util.List<String> getTagList() {
        if (this.tags == null || this.tags.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return java.util.Arrays.asList(this.tags.split(","));
    }
}