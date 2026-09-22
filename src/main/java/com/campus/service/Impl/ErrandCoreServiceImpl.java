package com.campus.service.Impl;

import com.campus.entity.ErrandOrder;
import com.campus.entity.ErrandTask;
import com.campus.mapper.ErrandOrderMapper;
import com.campus.mapper.ErrandTaskMapper;
import com.campus.service.IErrandCoreService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ErrandCoreServiceImpl implements IErrandCoreService {

    @Resource
    private ErrandTaskMapper taskMapper;

    @Resource
    private ErrandOrderMapper errandorderMapper;

    @Override
    public ErrandTask getTaskInfo(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    @Override
    public void updateTaskStatus(Long taskId, Integer status) {
        taskMapper.updateStatus(taskId, status);
    }

    @Override
    public ErrandOrder getOrderInfo(Long orderId) {
        return errandorderMapper.selectById(orderId);
    }

    @Override
    public void createOrder(ErrandTask task, Long acceptorId) {
        ErrandOrder order = new ErrandOrder();
        order.setTaskId(task.getId());
        order.setPublisherId(task.getPublisherId());
        order.setAcceptorId(acceptorId);
        order.setReward(task.getReward());
        order.setStatus(1);
        order.setPayStatus(0);
        errandorderMapper.insert(order);
    }
}