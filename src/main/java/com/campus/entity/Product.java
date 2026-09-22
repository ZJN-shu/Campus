package com.campus.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("tb_product")
public class Product {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long sellerId;

    @NotBlank(message = "商品标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度必须在2-100字之间")
    private String title;

    @Size(max = 5000, message = "描述长度不能超过5000字")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @DecimalMax(value = "999999.99", message = "价格不能超过999999.99")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "原价不能为负数")
    @DecimalMax(value = "999999.99", message = "原价不能超过999999.99")
    private BigDecimal originalPrice;

    @Size(max = 20, message = "分类名称长度不能超过20")
    private String category;

    @Size(max = 20, message = "子分类名称长度不能超过20")
    private String subCategory;

    @Size(max = 20, message = "成色描述长度不能超过20")
    private String quality;

    @Size(max = 2000, message = "图片链接长度不能超过2000")
    private String images;

    @Size(max = 100, message = "交易地点长度不能超过100")
    private String location;

    // 库存相关
    private Integer stock;
    private Integer reservedStock;
    private Integer version;

    private Integer viewCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 非数据库字段
    @TableField(exist = false)
    private String sellerName;

    @TableField(exist = false)
    private String sellerAvatar;

    @TableField(exist = false)
    private Integer sellerCredit;

    @TableField(exist = false)
    private Boolean isFavorited;

    // ==================== 辅助方法 ====================

    /**
     * 解析图片JSON为List
     */
    public List<String> getImageList() {
        if (this.images == null || this.images.isEmpty() || "[]".equals(this.images)) {
            return new ArrayList<>();
        }
        return com.alibaba.fastjson.JSON.parseArray(this.images, String.class);
    }

    /**
     * 获取第一张图片（用于列表展示）
     */
    public String getFirstImage() {
        List<String> imageList = getImageList();
        if (imageList == null || imageList.isEmpty()) {
            // 返回默认图片
            return "https://picsum.photos/300/200?random=" + (id != null ? id : 0);
        }
        return imageList.get(0);
    }

    /**
     * 设置图片列表
     */
    public void setImageList(List<String> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            this.images = "[]";
        } else {
            this.images = com.alibaba.fastjson.JSON.toJSONString(imageList);
        }
    }

    /**
     * 获取可用库存（实际库存 - 预扣库存）
     */

    public int getAvailableStock() {
        int s = stock != null ? stock : 0;
        int r = reservedStock != null ? reservedStock : 0;
        int result = s - r;
        return result;
    }
}