package com.campus.service;

import com.campus.dto.Result;

public interface IFollowService {

    /**
     * 关注
     */
    Result follow(Long followeeId);

    /**
     * 取消关注
     */
    Result unfollow(Long followeeId);

    /**
     * 是否已关注
     */
    Boolean isFollowed(Long followeeId);

    /**
     * 获取粉丝列表
     */
    Result getFollowers(Long userId, Integer current);

    /**
     * 获取关注列表
     */
    Result getFollowees(Long userId, Integer current);

    /**
     * 获取共同关注
     */
    Result getCommonFollows(Long userId);
}