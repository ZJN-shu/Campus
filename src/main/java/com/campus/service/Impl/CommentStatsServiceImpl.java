package com.campus.service.Impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.entity.Post;
import com.campus.mapper.PostMapper;
import com.campus.service.ICommentStatsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class CommentStatsServiceImpl implements ICommentStatsService {

    @Resource
    private PostMapper postMapper;

    @Override
    public void incrementPostCommentCount(Long postId) {
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count + 1"));
    }

    @Override
    public void decrementPostCommentCount(Long postId) {
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("comment_count = comment_count - 1"));
    }
}