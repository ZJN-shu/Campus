package com.campus.document;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostDocument {

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
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private Integer status;
    private LocalDateTime createTime;
}