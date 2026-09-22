package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;
import java.util.List;


public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 预扣库存（下单时调用）
     * 条件：可用库存 >= 购买数量
     */
    @Update("UPDATE tb_product " +
            "SET reserved_stock = reserved_stock + #{quantity}, version = version + 1 " +
            "WHERE id = #{id} " +
            "AND (stock - reserved_stock) >= #{quantity} " +
            "AND version = #{version}")
    int reserveStock(@Param("id") Long id,
                     @Param("quantity") Integer quantity,
                     @Param("version") Integer version);

    /**
     * 确认扣减库存（支付成功时调用）
     * 条件：预扣库存 >= 购买数量
     */
    @Update("UPDATE tb_product " +
            "SET stock = stock - #{quantity}, reserved_stock = reserved_stock - #{quantity} " +
            "WHERE id = #{id} " +
            "AND reserved_stock >= #{quantity}")
    int confirmStock(@Param("id") Long id,
                     @Param("quantity") Integer quantity);

    /**
     * 释放预扣库存（订单取消/超时时调用）
     */
    @Update("UPDATE tb_product " +
            "SET reserved_stock = reserved_stock - #{quantity} " +
            "WHERE id = #{id} " +
            "AND reserved_stock >= #{quantity}")
    int releaseReservedStock(@Param("id") Long id,
                             @Param("quantity") Integer quantity);

    /**
     * 查询商品（带乐观锁版本号）
     */
    @Select("SELECT id, title, price, stock, reserved_stock, version FROM tb_product WHERE id = #{id}")
    Product selectForUpdate(@Param("id") Long id);

    /**
     * 获取可用库存
     */
    @Select("SELECT (stock - reserved_stock) as available_stock FROM tb_product WHERE id = #{id}")
    Integer getAvailableStock(@Param("id") Long id);

    /**
     * 增加浏览次数
     */
    @Update("UPDATE tb_product SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);

    /**
     * 增加收藏数
     */
    @Update("UPDATE tb_product SET favorite_count = favorite_count + 1 WHERE id = #{id}")
    void incrementFavoriteCount(@Param("id") Long id);

    /**
     * 减少收藏数
     */
    @Update("UPDATE tb_product SET favorite_count = favorite_count - 1 WHERE id = #{id}")
    void decrementFavoriteCount(@Param("id") Long id);

    /**
     * 增加评论数
     */
    @Update("UPDATE tb_product SET comment_count = comment_count + 1 WHERE id = #{id}")
    void incrementCommentCount(@Param("id") Long id);

    /**
     * 按分类查询商品（带价格区间）
     */
    @Select("SELECT * FROM tb_product WHERE status = 1 " +
            "AND category = #{category} " +
            "AND price BETWEEN #{minPrice} AND #{maxPrice} " +
            "ORDER BY create_time DESC")
    List<Product> queryByCategory(@Param("category") String category,
                                  @Param("minPrice") BigDecimal minPrice,
                                  @Param("maxPrice") BigDecimal maxPrice);

    /**
     * 搜索商品（标题或描述）
     */
    @Select("SELECT * FROM tb_product WHERE status = 1 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY create_time DESC")
    List<Product> searchProducts(@Param("keyword") String keyword);

    @Update("UPDATE tb_product SET hot_score = #{score} WHERE id = #{id}")
    int updateHotScore(@Param("id") Long id, @Param("score") BigDecimal score);
}