package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.Post;
import com.campus.entity.PostComment;

public interface IPostService extends IService<Post> {

    /**
     * 查询帖子详情
     */
    Result queryPostById(Long id);

    /**
     * 按板块查询帖子
     */
    Result queryPostsByCategory(String category, Integer current, String sort);

    /**
     * 发布帖子
     */
    Result createPost(Post post);

    /**
     * 更新帖子
     */
    Result updatePost(Post post);

    /**
     * 删除帖子
     */
    Result deletePost(Long id);

    /**
     * 获取热门帖子
     */
    Result getHotPosts(String category, Integer limit);

    /**
     * 获取推荐帖子
     */
    Result getRecommendedPosts(Integer current);

    /**
     * 获取当前用户发布的帖子
     */
    Result getMyPosts(Integer current);
    /**
     * 发表评论
     */

    /**
     * 增加浏览次数
     */
    void incrementViewCount(Long id);
}