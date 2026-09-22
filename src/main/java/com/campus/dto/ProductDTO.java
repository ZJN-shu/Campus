package com.campus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductDTO {
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 卖家信息
    private Long sellerId;
    private String sellerName;
    private String sellerAvatar;
    private Integer sellerCredit;

    // 互动状态
    private Boolean isFavorited;
    private Boolean isLiked;

    // 获取第一张图片
    public String getFirstImage() {
        if (images != null && !images.isEmpty()) {
            return images.get(0);
        }
        return "https://picsum.photos/300/200?random=" + id;
    }
}