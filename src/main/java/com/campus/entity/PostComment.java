package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_post_comment")
public class PostComment {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    private Long userId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容长度不能超过2000字")
    private String content;
    private Integer likeCount;
    private Long parentId;
    private Long replyUserId;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;

    @TableField(exist = false)
    private String replyUserName;
}