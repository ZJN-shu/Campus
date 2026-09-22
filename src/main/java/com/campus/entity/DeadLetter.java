package com.campus.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeadLetter {
    private Long id;
    private String streamKey;
    private String message;
    private String errorMsg;
    private Integer retryCount;
    private Integer status;  // 0-待重试 1-已处理 2-最终失败
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}