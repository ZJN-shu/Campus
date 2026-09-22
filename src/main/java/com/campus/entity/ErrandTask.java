package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_errand_task")
public class ErrandTask {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long publisherId;

    @NotBlank(message = "任务标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度必须在2-100字之间")
    private String title;

    @Size(max = 2000, message = "任务描述长度不能超过2000字")
    private String description;

    @Size(max = 20, message = "任务分类长度不能超过20")
    private String category;

    @NotNull(message = "赏金不能为空")
    @DecimalMin(value = "0.01", message = "赏金必须大于0")
    @DecimalMax(value = "9999.99", message = "赏金不能超过9999.99")
    private BigDecimal reward;

    @Size(max = 200, message = "取件地点长度不能超过200")
    private String pickupLocation;

    @Size(max = 200, message = "送达地点长度不能超过200")
    private String deliveryLocation;
    private Double latitude;
    private Double longitude;
    private LocalDateTime deadline;
    private Integer status;
    private Long acceptorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime finishTime;

    // 非数据库字段
    @TableField(exist = false)
    private String publisherName;
    @TableField(exist = false)
    private String publisherAvatar;
    @TableField(exist = false)
    private String acceptorName;
}