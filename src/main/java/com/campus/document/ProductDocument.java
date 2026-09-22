package com.campus.document;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDocument {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String category;
    private String subCategory;
    private String quality;
    private List<String> images;
    private String location;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Integer status;

    // 卖家信息
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private Integer sellerCredit;

    // 时间字段
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 距离排序用（搜索时动态赋值，不存ES）
    private Double distance;
}