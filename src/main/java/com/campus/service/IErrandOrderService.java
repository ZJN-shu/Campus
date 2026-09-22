package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.ErrandOrder;
import org.springframework.transaction.annotation.Transactional;

public interface IErrandOrderService extends IService<ErrandOrder> {

    Result payOrder(Long orderId);

    Result getOrderDetail(Long orderId);

    Result getMyOrders(Integer current, Integer status);

    Result getOrderByTaskId(Long taskId);

    @Transactional
    Result refundOrder(Long orderId);
}