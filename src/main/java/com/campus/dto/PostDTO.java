package com.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private List<String> tags;
    private List<String> images;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer favoriteCount;
    private Double hotScore;
    private Boolean isTop;
    private Boolean isEssence;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 作者信息
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String authorCollege;

    // 互动状态
    private Boolean isLiked;
    private Boolean isFavorited;

    // 评论预览（前3条）
    private List<CommentDTO> topComments;
}