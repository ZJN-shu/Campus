package com.campus.service;

import com.campus.dto.Result;

public interface IFavoriteService {

    /**
     * 收藏
     */
    Result favorite(String targetType, Long targetId);

    /**
     * 取消收藏
     */
    Result unfavorite(String targetType, Long targetId);

    /**
     * 是否已收藏
     */
    Boolean isFavorited(String targetType, Long targetId);

    /**
     * 获取收藏数
     */
    Long getFavoriteCount(String targetType, Long targetId);

    /**
     * 查询我的收藏
     */
    Result queryMyFavorites(String targetType, Integer current);
}