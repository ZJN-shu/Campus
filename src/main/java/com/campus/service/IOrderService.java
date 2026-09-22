package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.Order;

/**
 * 订单服务接口
 */
public interface IOrderService extends IService<Order> {

    /**
     * 创建订单（预扣库存）
     * @param productId 商品ID
     * @param quantity 购买数量
     * @return 订单信息
     */
    Result createOrder(Long productId, Integer quantity);

    /**
     * 支付订单（确认扣减库存）
     * @param orderId 订单ID
     * @return 支付结果
     */
    Result payOrder(Long orderId);

    /**
     * 取消订单（释放预扣库存）
     * @param orderId 订单ID
     * @return 取消结果
     */
    Result cancelOrder(Long orderId);

    /**
     * 获取订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    Result getOrderDetail(Long orderId);

    /**
     * 获取我的订单列表
     * @param status 订单状态（0-待支付 1-已支付 2-已取消 3-已完成）
     * @param current 当前页码
     * @return 订单列表
     */
    Result getMyOrders(Integer status, Integer current);

    /**
     * 删除订单（软删除，只有已取消的订单可删除）
     * @param orderId 订单ID
     * @return 删除结果
     */
    Result deleteOrder(Long orderId);
}