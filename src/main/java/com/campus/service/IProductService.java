package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.Product;
import java.math.BigDecimal;

public interface IProductService extends IService<Product> {

    /**
     * 查询商品详情
     */
    Result queryProductById(Long id);

    /**
     * 按分类查询商品
     */
    Result queryProductByCategory(String category, Integer current,
                                  BigDecimal minPrice, BigDecimal maxPrice, String sort);

    /**
     * 发布商品
     */
    Result publishProduct(Product product);

    /**
     * 更新商品
     */
    Result updateProduct(Product product);

    /**
     * 删除商品
     */
    Result deleteProduct(Long id);

    /**
     * 搜索商品
     */
    Result searchProducts(String keyword, Integer current);

    /**
     * 获取当前用户发布的商品
     */
    Result getMyProducts(Integer current);

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);
}