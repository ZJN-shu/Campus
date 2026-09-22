package com.campus.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.CommentDTO;
import com.campus.dto.Result;
import com.campus.entity.PostComment;
import com.campus.entity.Student;
import com.campus.mapper.PostCommentMapper;
import com.campus.service.*;
import com.campus.service.ContentModerationService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment>
        implements IPostCommentService {

    @Resource
    private ICommentStatsService commentStatsService;  // 注入统计服务

    @Resource
    private IStudentService studentService;

    @Resource
    private IHotRankService hotRankService;

    @Resource
    private ContentModerationService contentModerationService;

    @Override
    public Result queryCommentsByPost(Long postId, Integer current) {
        // 分页查询顶级评论
        Page<PostComment> page = lambdaQuery()
                .eq(PostComment::getPostId, postId)
                .eq(PostComment::getParentId, 0)
                .eq(PostComment::getStatus, 1)
                .orderByDesc(PostComment::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        // 获取所有顶级评论ID
        List<Long> parentIds = page.getRecords().stream()
                .map(PostComment::getId)
                .collect(Collectors.toList());

        // 查询所有回复
        List<PostComment> allReplies = findRepliesByParentIds(postId, parentIds);

        // 按父评论分组
        Map<Long, List<PostComment>> repliesMap = allReplies.stream()
                .collect(Collectors.groupingBy(PostComment::getParentId));

        // 转换为DTO
        List<CommentDTO> dtoList = page.getRecords().stream()
                .map(comment -> convertToDTO(comment, repliesMap.get(comment.getId())))
                .collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    @Override
    public List<CommentDTO> getTopComments(Long postId, Integer limit) {
        List<PostComment> comments = lambdaQuery()
                .eq(PostComment::getPostId, postId)
                .eq(PostComment::getParentId, 0)
                .eq(PostComment::getStatus, 1)
                .orderByDesc(PostComment::getLikeCount)
                .last("LIMIT " + limit)
                .list();

        return comments.stream()
                .map(comment -> convertToDTO(comment, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Result addComment(PostComment comment) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 内容安全审核
        ContentModerationService.ModerationResult contentCheck = contentModerationService.moderate(comment.getContent(), true);
        if (!contentCheck.isPassed()) {
            return Result.fail("评论内容包含违规内容，请修改");
        }

        comment.setUserId(userId);
        comment.setLikeCount(0);
        comment.setStatus(1);

        boolean success = save(comment);
        if (!success) {
            return Result.fail("评论失败");
        }

        // 使用统计服务更新评论数
        commentStatsService.incrementPostCommentCount(comment.getPostId());

        // 记录热度
        hotRankService.recordAction("post", comment.getPostId(), "comment", userId);

        return Result.ok(comment.getId());
    }

    @Override
    @Transactional
    public Result deleteComment(Long id) {
        PostComment comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }

        Long currentUserId = StudentHolder.getStudentId();
        if (!comment.getUserId().equals(currentUserId)) {
            return Result.fail("无权删除");
        }

        comment.setStatus(2);
        boolean success = updateById(comment);

        if (success) {
            // 使用统计服务更新评论数
            commentStatsService.decrementPostCommentCount(comment.getPostId());
        }

        return success ? Result.ok() : Result.fail("删除失败");
    }
    /**
     * 查询回复列表
     */
    private List<PostComment> findRepliesByParentIds(Long postId, List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }

        return lambdaQuery()
                .eq(PostComment::getPostId, postId)
                .in(PostComment::getParentId, parentIds)
                .eq(PostComment::getStatus, 1)
                .orderByAsc(PostComment::getCreateTime)
                .list();
    }

    /**
     * 转换为DTO
     */
    private CommentDTO convertToDTO(PostComment comment, List<PostComment> replies) {
        CommentDTO dto = BeanUtil.copyProperties(comment, CommentDTO.class);

        // 查询评论者信息
        Student user = studentService.getById(comment.getUserId());
        if (user != null) {
            dto.setUserName(user.getNickName());
            dto.setUserAvatar(user.getAvatar());
        }

        // 查询被回复者信息
        if (comment.getReplyUserId() != null && comment.getReplyUserId() > 0) {
            Student replyUser = studentService.getById(comment.getReplyUserId());
            if (replyUser != null) {
                dto.setReplyUserName(replyUser.getNickName());
            }
        }

        // 设置回复
        if (replies != null && !replies.isEmpty()) {
            dto.setReplies(replies.stream()
                    .map(reply -> convertToDTO(reply, null))
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}