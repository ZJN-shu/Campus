package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_order")
public class Order {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    // 在 Order.java 中添加
    @TableField(exist = false)
    private String productTitle;

    @TableField(exist = false)
    private String productImage;
    private String orderNo;
    private Long productId;
    private Long userId;
    private Integer quantity;
    private BigDecimal amount;
    private Integer status;        // 0-待支付 1-已支付 2-已取消 3-已完成
    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime payTime;
}