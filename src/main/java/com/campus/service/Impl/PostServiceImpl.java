package com.campus.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.cache.MultiLevelCache;
import com.campus.dto.CommentDTO;
import com.campus.dto.PostDTO;
import com.campus.dto.Result;
import com.campus.entity.Post;
import com.campus.entity.Student;
import com.campus.mapper.PostMapper;
import com.campus.processor.AsyncEventProcessor;  // 新增：消息队列处理器
import com.campus.service.*;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.campus.utils.RedisConstants.*;

@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post>
        implements IPostService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MultiLevelCache multiLevelCache;

    @Resource
    private IStudentService studentService;

    @Resource
    private IPostCommentService commentService;

    @Resource
    private ILikeService likeService;

    @Resource
    private IStatsService statsService;

    @Resource
    private IFavoriteService favoriteService;

    @Resource
    private IHotRankService hotRankService;

    @Resource
    private AsyncEventProcessor asyncEventProcessor;  // 新增：注入消息队列处理器

    @Resource
    private ContentModerationService contentModerationService;

    // ==================== 查询方法 ====================

    @Override
    public Result queryPostById(Long id) {
        String cacheKey = CACHE_POST_KEY + id;

        // 使用多级缓存（本地缓存 + Redis）
        String postJson = multiLevelCache.get(cacheKey, key -> {
            Post post = getById(id);
            if (post == null) {
                return null;
            }
            return JSON.toJSONString(post);
        }, CACHE_POST_TTL);

        if (postJson == null) {
            return Result.fail("帖子不存在");
        }

        Post post = JSON.parseObject(postJson, Post.class);
        PostDTO dto = convertToDTO(post);

        // 发送浏览事件到消息队列（异步处理，不影响响应时间）
        Long userId = StudentHolder.getStudentId();
        if (userId != null) {
            asyncEventProcessor.sendViewEvent(id, userId, "post");
        }

        return Result.ok(dto);
    }

    // ==================== 更新/删除方法 ====================

    @Override
    @Transactional
    public Result updatePost(Post post) {
        Post old = getById(post.getId());
        if (old == null) {
            return Result.fail("帖子不存在");
        }

        Long currentUserId = StudentHolder.getStudentId();
        if (!old.getUserId().equals(currentUserId)) {
            return Result.fail("无权修改");
        }

        boolean success = updateById(post);
        if (success) {
            multiLevelCache.evict(CACHE_POST_KEY + post.getId());
        }

        return success ? Result.ok() : Result.fail("修改失败");
    }

    @Override
    @Transactional
    public Result deletePost(Long id) {
        Post post = getById(id);
        if (post == null) {
            return Result.fail("帖子不存在");
        }

        Long currentUserId = StudentHolder.getStudentId();
        if (!post.getUserId().equals(currentUserId)) {
            return Result.fail("无权删除");
        }

        boolean success = removeById(id);
        if (success) {
            multiLevelCache.evict(CACHE_POST_KEY + id);
            stringRedisTemplate.opsForZSet().remove(HOT_RANK_POST, "post:" + id);
        }

        return success ? Result.ok() : Result.fail("删除失败");
    }

    // ==================== 其他方法 ====================

    @Override
    public Result queryPostsByCategory(String category, Integer current, String sort) {
        Page<Post> page = lambdaQuery()
                .eq(Post::getStatus, 1)
                .eq(StrUtil.isNotBlank(category) && !"all".equals(category),
                        Post::getCategory, category)
                .orderByDesc(Post::getIsTop)
                .orderByDesc("hot".equals(sort), Post::getHotScore)
                .orderByDesc("time".equals(sort), Post::getCreateTime)
                .orderByDesc("essence".equals(sort), Post::getIsEssence)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<PostDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    @Override
    @Transactional
    public Result createPost(Post post) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 内容安全审核（严格模式：标题/内容不允许包含敏感词）
        log.info("开始内容审核，标题: {}", post.getTitle());
        ContentModerationService.ModerationResult titleCheck = contentModerationService.moderate(post.getTitle(), true);
        log.info("标题审核结果: passed={}, hitWords={}", titleCheck.isPassed(), titleCheck.getHitWords());
        if (!titleCheck.isPassed()) {
            return Result.fail("标题包含违规内容，请修改");
        }
        ContentModerationService.ModerationResult contentCheck = contentModerationService.moderate(post.getContent(), false);
        log.info("内容审核结果: passed={}, hitWords={}", contentCheck.isPassed(), contentCheck.getHitWords());
        if (!contentCheck.isPassed()) {
            return Result.fail("内容包含违规内容，请修改");
        }
        // 非严格模式自动替换后的内容
        post.setContent(contentCheck.getCleanedContent());

        post.setUserId(userId);
        post.setStatus(1);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setHotScore(BigDecimal.ZERO);

        boolean success = save(post);
        if (!success) {
            return Result.fail("发布失败");
        }

        return Result.ok(post.getId());
    }

    @Override
    public Result getHotPosts(String category, Integer limit) {
        List<Post> posts = baseMapper.queryHotPosts(category, limit);
        List<PostDTO> dtoList = posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return Result.ok(dtoList);
    }

    @Override
    public Result getRecommendedPosts(Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return getHotPosts("all", 20);
        }
        return queryPostsByCategory("all", current, "time");
    }

    @Override
    public Result getMyPosts(Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }
        Page<Post> page = lambdaQuery()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 1)
                .orderByDesc(Post::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        List<PostDTO> dtoList = page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.ok(dtoList, page.getTotal());
    }

    @Override
    public void incrementViewCount(Long id) {
        // 注意：这个方法现在主要用于兼容性，实际浏览计数已通过消息队列处理
        // 保留用于其他地方的调用
        String key = POST_STATS_KEY + id;
        stringRedisTemplate.opsForHash().increment(key, "viewCount", 1);
    }

    /**
     * 转换为DTO
     */
    private PostDTO convertToDTO(Post post) {
        PostDTO dto = BeanUtil.copyProperties(post, PostDTO.class);

        if (StrUtil.isNotBlank(post.getTags())) {
            dto.setTags(java.util.Arrays.asList(post.getTags().split(",")));
        }

        dto.setImages(post.getImageList());

        Student author = studentService.getById(post.getUserId());
        if (author != null) {
            dto.setAuthorName(author.getNickName());
            dto.setAuthorAvatar(author.getAvatar());
            dto.setAuthorCollege(author.getCollege());
        }

        Long currentUserId = StudentHolder.getStudentId();
        if (currentUserId != null) {
            dto.setIsLiked(likeService.isLiked("post", post.getId()));
            dto.setIsFavorited(favoriteService.isFavorited("post", post.getId()));
        }

        List<CommentDTO> topComments = commentService.getTopComments(post.getId(), 3);
        dto.setTopComments(topComments);

        return dto;
    }
}