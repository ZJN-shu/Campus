package com.campus.controller;

import com.campus.annotation.AuditLog;
import com.campus.dto.Result;
import com.campus.entity.ErrandTask;
import com.campus.service.IErrandTaskService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/errand")
public class ErrandTaskController {

    @Resource
    private IErrandTaskService errandTaskService;

    /**
     * 发布跑腿任务
     */
    @PostMapping("/task")
    @AuditLog(module = "跑腿", operation = "发布任务")
    public Result publishTask(@Valid @RequestBody ErrandTask task) {
        return errandTaskService.publishTask(task);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/task/{id}")
    public Result getTaskDetail(@PathVariable Long id) {
        return errandTaskService.getTaskDetail(id);
    }

    /**
     * 获取附近任务
     */
    @GetMapping("/tasks/nearby")
    public Result getNearbyTasks(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "1") Integer current) {
        return errandTaskService.getNearbyTasks(latitude, longitude, current);
    }

    /**
     * 接单
     */
    @PostMapping("/task/{id}/accept")
    @AuditLog(module = "跑腿", operation = "接单")
    public Result acceptTask(@PathVariable Long id) {
        return errandTaskService.acceptTask(id);
    }

    /**
     * 完成任务
     */
    @PutMapping("/task/{id}/complete")
    public Result completeTask(@PathVariable Long id) {
        return errandTaskService.completeTask(id);
    }

    /**
     * 取消任务
     */
    @DeleteMapping("/task/{id}")
    public Result cancelTask(@PathVariable Long id) {
        return errandTaskService.cancelTask(id);
    }

    /**
     * 我发布的任务
     */
    @GetMapping("/tasks/published")
    public Result getMyPublishedTasks(@RequestParam(defaultValue = "1") Integer current) {
        return errandTaskService.getMyPublishedTasks(current);
    }

    /**
     * 我接单的任务
     */
    @GetMapping("/tasks/accepted")
    public Result getMyAcceptedTasks(@RequestParam(defaultValue = "1") Integer current) {
        return errandTaskService.getMyAcceptedTasks(current);
    }
}