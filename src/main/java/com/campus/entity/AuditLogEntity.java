package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_audit_log")
public class AuditLogEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String params;
    private String ip;
    private Integer status;
    private String errorMsg;
    private Long duration;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
