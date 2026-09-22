package com.campus.service;

public interface IErrandStatsService {
    void updateTaskStatusAfterPay(Long taskId);
    void updateTaskStatusAfterComplete(Long taskId);
}