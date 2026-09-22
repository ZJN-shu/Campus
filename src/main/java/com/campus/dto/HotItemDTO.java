package com.campus.dto;

import lombok.Data;

@Data
public class HotItemDTO {
    private Long id;
    private String type;        // post/product
    private String title;
    private String content;
    private Double hotScore;
    private Integer rank;

    // 作者信息
    private Long authorId;
    private String authorName;
    private String authorAvatar;

    // 统计数据
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    // 趋势（上升/下降）
    private Double trend;

    // 封面图
    private String coverImage;
}