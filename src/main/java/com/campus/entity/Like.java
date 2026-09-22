package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_like")
public class Like {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String targetType;
    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}