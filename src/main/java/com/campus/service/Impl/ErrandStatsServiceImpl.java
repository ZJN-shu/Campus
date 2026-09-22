package com.campus.service.Impl;


import com.campus.entity.ErrandTask;
import com.campus.mapper.ErrandTaskMapper;
import com.campus.service.IErrandStatsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class ErrandStatsServiceImpl implements IErrandStatsService {

    @Resource
    private ErrandTaskMapper errandTaskMapper;

    @Override
    public void updateTaskStatusAfterPay(Long taskId) {
        errandTaskMapper.updateStatus(taskId, 3); // 进行中
    }

    @Override
    public void updateTaskStatusAfterComplete(Long taskId) {
        errandTaskMapper.updateStatus(taskId, 4); // 已完成
    }
}