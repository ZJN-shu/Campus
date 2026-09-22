package com.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO {
    private Long id;
    private String content;
    private Integer likeCount;
    private Long parentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 评论者信息
    private Long userId;
    private String userName;
    private String userAvatar;

    // 回复对象信息
    private Long replyUserId;
    private String replyUserName;

    // 互动状态
    private Boolean isLiked;

    // 子评论（用于嵌套展示）
    private List<CommentDTO> replies;
}