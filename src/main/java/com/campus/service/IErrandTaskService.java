package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.ErrandTask;

public interface IErrandTaskService extends IService<ErrandTask> {

    Result publishTask(ErrandTask task);

    Result getTaskDetail(Long taskId);

    Result getNearbyTasks(Double latitude, Double longitude, Integer current);

    Result acceptTask(Long taskId);

    Result completeTask(Long taskId);

    Result cancelTask(Long taskId);

    Result getMyPublishedTasks(Integer current);

    Result getMyAcceptedTasks(Integer current);
}