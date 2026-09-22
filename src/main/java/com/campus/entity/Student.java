package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_student")
public class Student {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String studentNo;
    private String phone;
    private String password;
    private String realName;
    private String nickName;
    private String avatar;
    private String college;
    private String major;
    private Integer grade;
    private Integer credit;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}