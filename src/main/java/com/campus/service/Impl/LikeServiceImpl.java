package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dto.Result;
import com.campus.entity.Like;
import com.campus.entity.Post;
import com.campus.health.RedisHealthChecker;
import com.campus.mapper.LikeMapper;
import com.campus.mapper.PostMapper;
import com.campus.processor.AsyncEventProcessor;
import com.campus.processor.ReliableEventProducer;
import com.campus.service.FallbackService;
import com.campus.service.IHotRankService;
import com.campus.service.ILikeService;
import com.campus.utils.StudentHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.campus.utils.RedisConstants.*;

@Service
@Slf4j
public class LikeServiceImpl implements ILikeService {

    @Resource
    private LikeMapper likeMapper;

    @Resource
    private PostMapper postMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IHotRankService hotRankService;
    @Resource
    private AsyncEventProcessor asyncEventProcessor;
    @Resource
    private ReliableEventProducer reliableEventProducer;  // 新增：可靠生产者

    @Resource
    private RedisHealthChecker redisHealthChecker;  // 新增：健康检查

    @Resource
    private FallbackService fallbackService;  // 新增：降级服务


    @Override
    @Transactional
    public Result like(String targetType, Long targetId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 检查是否已点赞（查数据库，保证不重复）
        Long count = likeMapper.selectCount(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));

        if (count > 0) {
            return Result.fail("已经点赞过了");
        }

        // 2. 保存点赞记录到数据库（保证数据不丢失）
        Like like = new Like();
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        likeMapper.insert(like);

        // 3. 根据 Redis 健康状态决定处理方式
        if (!redisHealthChecker.isHealthy()) {
            // Redis 不可用，降级处理：只写数据库，不处理缓存和热榜
            log.warn("Redis不可用，点赞已保存到数据库，后续同步");
            return Result.ok("点赞成功");
        }

        // 4. Redis 可用，正常处理
        try {
            // 4.1 更新 Redis 缓存（用于快速查询点赞状态）
            String key = "like:" + targetType + ":" + targetId;
            stringRedisTemplate.opsForSet().add(key, userId.toString());

            // 4.2 发送异步消息（带重试机制）
            reliableEventProducer.sendLikeEvent(targetId, userId, targetType, "add");

            // 4.3 记录热度（可选，也可以异步处理）
            hotRankService.recordAction(targetType, targetId, "like", userId);

            log.debug("点赞成功: userId={}, targetType={}, targetId={}", userId, targetType, targetId);

        } catch (Exception e) {
            log.error("Redis操作失败，进入降级处理", e);
            // 降级：存入本地队列，等待恢复后处理
            fallbackService.saveToLocalQueue(targetId, userId, targetType, "add");
            return Result.ok("点赞已记录，稍后同步");
        }

        return Result.ok();
    }

    //   @Override
//    @Transactional
//    public Result like(String targetType, Long targetId) {
//        Long userId = StudentHolder.getStudentId();
//        if (userId == null) {
//            return Result.fail("请先登录");
//        }
//
//        // 1. 检查是否已点赞
//        Long count = likeMapper.selectCount(new LambdaQueryWrapper<Like>()
//                .eq(Like::getUserId, userId)
//                .eq(Like::getTargetType, targetType)
//                .eq(Like::getTargetId, targetId));
//
//        if (count > 0) {
//            return Result.fail("已经点赞过了");
//        }
//
//        // 2. 保存点赞记录
//        String key = "like:" + targetType + ":" + targetId;
//        stringRedisTemplate.opsForSet().add(key, userId.toString());
//
//        // 发送异步事件（不立即写数据库）
//        asyncEventProcessor.sendLikeEvent(targetId, userId, targetType, "add");
//
//        return Result.ok();
////        Like like = new Like();
////        like.setUserId(userId);
////        like.setTargetType(targetType);
////        like.setTargetId(targetId);
////        likeMapper.insert(like);
////
////        // 3. 更新目标点赞数
////        updateTargetLikeCount(targetType, targetId, 1);
////
////        // 4. Redis记录
////        String key = getLikeKey(targetType, targetId);
////        stringRedisTemplate.opsForSet().add(key, userId.toString());
////
////        // 5. 记录热度
////        hotRankService.recordAction(targetType, targetId, "like", userId);
////
////        return Result.ok();
//    }

    @Override
    @Transactional
    public Result unlike(String targetType, Long targetId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 删除点赞记录
        likeMapper.delete(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getTargetType, targetType)
                .eq(Like::getTargetId, targetId));

        // 2. 更新目标点赞数
        updateTargetLikeCount(targetType, targetId, -1);

        // 3. Redis移除
        String key = getLikeKey(targetType, targetId);
        stringRedisTemplate.opsForSet().remove(key, userId.toString());

        return Result.ok();
    }

    @Override
    public Boolean isLiked(String targetType, Long targetId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return false;
        }

        String key = getLikeKey(targetType, targetId);
        Boolean isMember = stringRedisTemplate.opsForSet()
                .isMember(key, userId.toString());
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public Long getLikeCount(String targetType, Long targetId) {
        String key = getLikeKey(targetType, targetId);
        Long size = stringRedisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    /**
     * 更新目标点赞数
     */
    private void updateTargetLikeCount(String targetType, Long targetId, int delta) {
        if ("post".equals(targetType)) {
            postMapper.updateLikeCount(targetId, delta);
        }
        // 可以扩展其他类型
    }

    /**
     * 获取Redis Key
     */
    private String getLikeKey(String targetType, Long targetId) {
        switch (targetType) {
            case "post":
                return LIKE_POST_KEY + targetId;
            case "comment":
                return LIKE_COMMENT_KEY + targetId;
            case "product":
                return LIKE_PRODUCT_KEY + targetId;
            default:
                return "like:" + targetType + ":" + targetId;
        }
    }

}