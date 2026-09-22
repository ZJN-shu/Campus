package com.campus.service;

import com.campus.dto.Result;

public interface IHotRankService {

    /**
     * 获取热榜
     */
    Result getHotRank(String category, Integer current);

    /**
     * 获取24小时热榜
     */
    Result getTodayHot();

    /**
     * 获取上升最快榜
     */
    Result getRisingHot();

    /**
     * 记录用户行为（用于热度计算）
     */
    void recordAction(String targetType, Long targetId, String actionType, Long userId);

    /**
     * 定时重算热度分（应用时间衰减）
     * 由 ScheduledTask 每5分钟调用一次
     */
    void recalculateHotScores();
}