package com.campus.service;

public interface IStatsService {
    void incrementPostFavoriteCount(Long postId);
    void decrementPostFavoriteCount(Long postId);
    void incrementProductFavoriteCount(Long productId);
    void decrementProductFavoriteCount(Long productId);
}