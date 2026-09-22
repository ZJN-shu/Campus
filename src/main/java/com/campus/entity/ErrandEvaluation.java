package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_errand_evaluation")
public class ErrandEvaluation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long fromUserId;
    private Long toUserId;
    private Integer score;
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}