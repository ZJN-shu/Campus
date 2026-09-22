package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.CommentDTO;
import com.campus.dto.Result;
import com.campus.entity.PostComment;
import java.util.List;

public interface IPostCommentService extends IService<PostComment> {

    /**
     * 查询帖子评论
     */
    Result queryCommentsByPost(Long postId, Integer current);

    /**
     * 获取前几条评论
     */
    List<CommentDTO> getTopComments(Long postId, Integer limit);

    /**
     * 添加评论
     */
    Result addComment(PostComment comment);

    /**
     * 删除评论
     */
    Result deleteComment(Long id);
}