package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询超时未支付的订单
     */
    @Select("SELECT * FROM tb_order WHERE status = 0 AND expire_time < NOW()")
    List<Order> selectTimeoutOrders();

    /**
     * 更新订单状态
     */
    @Update("UPDATE tb_order SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 支付成功更新
     */
    @Update("UPDATE tb_order SET status = 1, pay_time = NOW() WHERE id = #{id} AND status = 0")
    int paySuccess(@Param("id") Long id);
}