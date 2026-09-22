package com.campus.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentDTO {
    private Long id;

    @Size(max = 20, message = "学号长度不能超过20")
    private String studentNo;

    @Size(max = 11, message = "手机号长度不能超过11")
    private String phone;

    @Size(max = 20, message = "真实姓名长度不能超过20")
    private String realName;

    @Size(max = 20, message = "昵称长度不能超过20")
    private String nickName;

    @Size(max = 500, message = "头像链接长度不能超过500")
    private String avatar;

    @Size(max = 30, message = "学院名称长度不能超过30")
    private String college;

    @Size(max = 30, message = "专业名称长度不能超过30")
    private String major;

    private Integer grade;
    private Integer credit;
    private Integer status;
    private LocalDateTime createTime;

    private String token;

    public StudentDTO() {
    }

    public StudentDTO(String token) {
        this.token = token;
    }
}