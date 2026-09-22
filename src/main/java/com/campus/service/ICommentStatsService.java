package com.campus.service;

public interface ICommentStatsService {
    void incrementPostCommentCount(Long postId);
    void decrementPostCommentCount(Long postId);
}