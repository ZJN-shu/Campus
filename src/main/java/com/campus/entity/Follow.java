package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_follow")
public class Follow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关注者ID */
    private Long userId;

    /** 被关注者ID */
    private Long followeeId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
