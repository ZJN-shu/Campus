package com.campus.service;

import com.campus.dto.Result;

public interface ILikeService {

    /**
     * 点赞
     */
    Result like(String targetType, Long targetId);

    /**
     * 取消点赞
     */
    Result unlike(String targetType, Long targetId);

    /**
     * 是否已点赞
     */
    Boolean isLiked(String targetType, Long targetId);

    /**
     * 获取点赞数
     */
    Long getLikeCount(String targetType, Long targetId);
}