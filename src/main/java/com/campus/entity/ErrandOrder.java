package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_errand_order")
public class ErrandOrder {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long publisherId;
    private Long acceptorId;
    private BigDecimal reward;
    private Integer status;      // 1-待支付 2-已支付 3-已完成 4-已退款
    private Integer payStatus;   // 0-未支付 1-已支付
    private LocalDateTime payTime;

    @TableField(exist = false)
    private String taskTitle;

    @TableField(exist = false)
    private Integer taskStatus;

    // 评价状态
    private Integer publisherEvaluated;  // 发布者是否已评价 0-未评价 1-已评价
    private Integer acceptorEvaluated;   // 接单者是否已评价 0-未评价 1-已评价

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}