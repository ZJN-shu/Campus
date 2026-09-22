package com.campus.service;

import com.campus.entity.ErrandOrder;
import com.campus.entity.ErrandTask;

public interface IErrandCoreService {

    /**
     * 获取任务信息
     */
    ErrandTask getTaskInfo(Long taskId);

    /**
     * 更新任务状态
     */
    void updateTaskStatus(Long taskId, Integer status);

    /**
     * 获取订单信息
     */
    ErrandOrder getOrderInfo(Long orderId);

    /**
     * 创建订单
     */
    void createOrder(ErrandTask task, Long acceptorId);
}