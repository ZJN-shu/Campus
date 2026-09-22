package com.campus.controller;

import com.campus.annotation.AuditLog;
import com.campus.annotation.RateLimit;
import com.campus.dto.PostDTO;
import com.campus.dto.Result;
import com.campus.entity.Post;
import com.campus.entity.PostComment;
import com.campus.service.ILikeService;
import com.campus.service.IPostCommentService;
import com.campus.service.IPostService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private IPostService postService;

    @Resource
    private IPostCommentService commentService;

    /**
     * 查询帖子详情
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable Long id) {
        return postService.queryPostById(id);
    }

    /**
     * 按板块查询帖子
     */
    @GetMapping("/category")
    public Result queryByCategory(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "hot") String sort) {
        return postService.queryPostsByCategory(category, current, sort);
    }

    /**
     * 发布帖子
     */
    @PostMapping
    @AuditLog(module = "帖子", operation = "发布")
    @RateLimit(value = 3, timeout = 1, timeUnit = TimeUnit.MINUTES,
            message = "发布太频繁，请稍后再试")
    public Result create(@Valid @RequestBody Post post) {
        return postService.createPost(post);
    }

    /**
     * 更新帖子
     */
    @PutMapping
    @AuditLog(module = "帖子", operation = "更新")
    public Result update(@Valid @RequestBody Post post) {
        return postService.updatePost(post);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "帖子", operation = "删除")
    public Result delete(@PathVariable Long id) {
        return postService.deletePost(id);
    }

    /**
     * 获取热门帖子
     */
    @GetMapping("/hot")
    public Result getHotPosts(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "20") Integer limit) {
        return postService.getHotPosts(category, limit);
    }

    /**
     * 获取推荐帖子
     */
    @GetMapping("/recommend")
    public Result getRecommendedPosts(
            @RequestParam(defaultValue = "1") Integer current) {
        return postService.getRecommendedPosts(current);
    }

    /**
     * 我发布的帖子
     */
    @GetMapping("/my")
    public Result getMyPosts(@RequestParam(defaultValue = "1") Integer current) {
        return postService.getMyPosts(current);
    }

    /**
     * 获取帖子评论
     */
    @GetMapping("/{id}/comments")
    public Result getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer current) {
        return commentService.queryCommentsByPost(id, current);
    }
    @Resource
    private ILikeService likeService;

    /**
     * 点赞帖子
     */
    @PostMapping("/like/{id}")
    @RateLimit(value = 5, timeout = 1, message = "点赞太频繁，请稍后再试")
    public Result likePost(@PathVariable Long id) {
        return likeService.like("post", id);
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/like/{id}")
    public Result unlikePost(@PathVariable Long id) {
        return likeService.unlike("post", id);
    }


    /**
     * 发表评论 - 每秒最多3次
     */
    @PostMapping("/comment")
    @AuditLog(module = "评论", operation = "发表")
    @RateLimit(value = 3, timeout = 1, message = "评论太频繁，请稍后再试")
    public Result addComment(@Valid @RequestBody PostComment comment) {
        return commentService.addComment(comment);
    }
    /**
     * 删除评论
     */
    @DeleteMapping("/comment/{id}")
    public Result deleteComment(@PathVariable Long id) {
        return commentService.deleteComment(id);
    }
}