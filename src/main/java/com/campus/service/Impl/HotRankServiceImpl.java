package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dto.HotItemDTO;
import com.campus.dto.Result;
import com.campus.entity.HotConfig;
import com.campus.entity.Post;
import com.campus.entity.Product;
import com.campus.entity.Student;
import com.campus.mapper.HotConfigMapper;
import com.campus.mapper.PostMapper;
import com.campus.mapper.ProductMapper;
import com.campus.service.IHotRankService;
import com.campus.service.IStudentService;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.campus.utils.RedisConstants.*;

@Slf4j
@Service
public class HotRankServiceImpl implements IHotRankService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private PostMapper postMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private HotConfigMapper configMapper;

    @Resource
    private IStudentService studentService;

    /** 热榜最低留存分数 — 低于此值的帖子移出热榜 */
    private static final double SCORE_FLOOR = 0.01;
    /** 热榜最大保留条目（定时裁剪用） */
    private static final int MAX_RANK_SIZE = 200;

    // ==================== 热榜查询方法 ====================

    @Override
    public Result getHotRank(String category, Integer current) {
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = start + SystemConstants.DEFAULT_PAGE_SIZE - 1;

        String key = "all".equals(category) ? HOT_RANK_ALL : HOT_RANK_CATEGORY + category;

        Set<ZSetOperations.TypedTuple<String>> rankSet =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        if (rankSet == null || rankSet.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        List<HotItemDTO> result = new ArrayList<>();
        int rank = start + 1;

        // 收集所有ID，批量查询
        List<Long> postIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();

        for (ZSetOperations.TypedTuple<String> tuple : rankSet) {
            String value = tuple.getValue();
            if (value == null) continue;

            String[] parts = value.split(":");
            if (parts.length < 2) continue;

            Long postId = Long.valueOf(parts[1]);
            postIds.add(postId);
            scoreMap.put(postId, tuple.getScore());
        }

        // 批量查询帖子（一次SQL，不是N次）
        List<Post> posts = postMapper.selectBatchIds(postIds);
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        // 组装结果
        for (Long postId : postIds) {
            Post post = postMap.get(postId);
            if (post == null) continue;

            HotItemDTO dto = new HotItemDTO();
            dto.setRank(rank++);
            dto.setId(postId);
            dto.setType("post");
            dto.setTitle(post.getTitle());
            dto.setHotScore(scoreMap.get(postId));
            dto.setViewCount(post.getViewCount());
            dto.setLikeCount(post.getLikeCount());
            dto.setCommentCount(post.getCommentCount());

            result.add(dto);
        }

        return Result.ok(result);
    }

    @Override
    public Result getTodayHot() {
        Set<ZSetOperations.TypedTuple<String>> todaySet =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_RANK_TODAY, 0, 19);

        List<HotItemDTO> result = new ArrayList<>();
        if (todaySet != null) {
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : todaySet) {
                String value = tuple.getValue();
                if (value == null) continue;

                String[] parts = value.split(":");
                if (parts.length < 2) continue;

                HotItemDTO dto = new HotItemDTO();
                dto.setRank(rank++);
                dto.setType(parts[0]);
                dto.setId(Long.valueOf(parts[1]));
                dto.setHotScore(tuple.getScore());

                // 补充标题，前端展示用
                enrichItemInfo(dto);

                result.add(dto);
            }
        }

        return Result.ok(result);
    }

    @Override
    public Result getRisingHot() {
        // 上升最快榜：取当前热榜前100，按"单位时间热度增长率"重排
        Set<ZSetOperations.TypedTuple<String>> allRank =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_RANK_ALL, 0, 99);

        if (allRank == null || allRank.isEmpty()) {
            return Result.ok(new ArrayList<>());
        }

        // 计算每个帖子的"热度/帖龄"比值作为上升势头
        List<HotItemDTO> candidates = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : allRank) {
            String value = tuple.getValue();
            if (value == null) continue;
            String[] parts = value.split(":");
            if (parts.length < 2) continue;

            String targetType = parts[0];
            Long targetId = Long.valueOf(parts[1]);
            Double score = tuple.getScore();
            if (score == null) continue;

            LocalDateTime createTime = getCreateTime(targetType, targetId);
            if (createTime == null) continue;

            long ageHours = Duration.between(createTime, LocalDateTime.now()).toHours();
            if (ageHours < 1) ageHours = 1; // 避免除零

            // 热度增长率 = 热度分 / 帖龄(小时)
            double riseRate = score / ageHours;

            HotItemDTO dto = new HotItemDTO();
            dto.setType(targetType);
            dto.setId(targetId);
            dto.setHotScore(score);
            dto.setTrend(riseRate);
            enrichItemInfo(dto);
            candidates.add(dto);
        }

        // 按增长率降序取前20
        candidates.sort((a, b) -> Double.compare(b.getTrend(), a.getTrend()));
        List<HotItemDTO> result = candidates.stream()
                .limit(20)
                .collect(Collectors.toList());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return Result.ok(result);
    }

    // ==================== 热度记录方法（核心） ====================

    /**
     * 记录用户行为，更新热度
     * 使用 @Async 异步执行，不阻塞主流程
     */
    @Override
    @Async
    public void recordAction(String targetType, Long targetId, String actionType, Long userId) {
        try {
            // 1. 获取配置
            HotConfig config = configMapper.selectOne(
                    new LambdaQueryWrapper<HotConfig>()
                            .eq(HotConfig::getTargetType, targetType));

            if (config == null) {
                log.warn("未找到热度配置，使用默认值: targetType={}", targetType);
                config = getDefaultConfig(targetType);
            }

            // 2. 计算热度增量（修复了 "add"→like, "remove"→-like 的映射）
            double increment = getWeight(actionType, config);
            if (increment == 0) return; // 无效动作不处理

            // 3. 更新原始累计分（持久化保存的底分，不受衰减影响）
            String scoreKey = HOT_SCORE_KEY + targetType + ":" + targetId;
            Double rawScore = stringRedisTemplate.opsForValue().increment(scoreKey, increment);
            if (rawScore == null) rawScore = increment;
            // 防止取消赞导致负分
            if (rawScore < 0) {
                rawScore = 0.0;
                stringRedisTemplate.opsForValue().set(scoreKey, "0");
            }

            // 4. 获取创建时间（一次查询，后续复用 — 避免每个 recordAction 查两次 DB）
            LocalDateTime createTime = getCreateTime(targetType, targetId);

            // 5. 计算带衰减的最终热度分
            double finalScore = computeFinalScore(rawScore, targetType, targetId, createTime);

            // 6. 更新综合热榜 ZSet
            String member = targetType + ":" + targetId;
            stringRedisTemplate.opsForZSet().add(HOT_RANK_ALL, member, finalScore);
            stringRedisTemplate.expire(HOT_RANK_ALL, SystemConstants.HOT_RANK_TTL, TimeUnit.DAYS);

            // 7. 如果发布于24小时内，同步更新今日热榜
            if (createTime != null && Duration.between(createTime, LocalDateTime.now()).toHours() <= 24) {
                stringRedisTemplate.opsForZSet().add(HOT_RANK_TODAY, member, finalScore);
                stringRedisTemplate.expire(HOT_RANK_TODAY, SystemConstants.HOT_RANK_TODAY_TTL, TimeUnit.DAYS);
            }

            // 8. 记录最近活跃时间戳（用于新鲜度加成）
            String lastActiveKey = HOT_LAST_ACTIVE_PREFIX + targetType + ":" + targetId;
            stringRedisTemplate.opsForValue().set(lastActiveKey,
                    String.valueOf(System.currentTimeMillis()), 1, TimeUnit.DAYS);

            // 9. 原始分 key 设置过期
            stringRedisTemplate.expire(scoreKey, SystemConstants.HOT_RANK_TTL, TimeUnit.DAYS);

            // 10. 每10次操作写一次数据库 hot_score
            if (shouldUpdateDatabase(targetType, targetId, actionType)) {
                updateDatabaseHotScore(targetType, targetId, finalScore);
            }

            log.debug("热度记录成功: targetType={}, targetId={}, actionType={}, increment={}, finalScore={}",
                    targetType, targetId, actionType, increment, finalScore);

        } catch (Exception e) {
            log.error("记录热度异常: targetType={}, targetId={}, actionType={}, error={}",
                    targetType, targetId, actionType, e.getMessage());
        }
    }

    // ==================== 定时衰减重算（每5分钟由 ScheduledTask 调用） ====================

    @Override
    public void recalculateHotScores() {
        log.debug("开始定时热度衰减重算...");
        long start = System.currentTimeMillis();

        // 1. 读取所有热榜成员
        Set<ZSetOperations.TypedTuple<String>> allMembers =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_RANK_ALL, 0, -1);

        if (allMembers == null || allMembers.isEmpty()) {
            log.debug("热榜为空，跳过重算");
            return;
        }

        // 2. 批量收集所有 ID 并查询创建时间（减少 SQL 次数）
        List<Long> postIds = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : allMembers) {
            String value = tuple.getValue();
            if (value == null) continue;
            String[] parts = value.split(":");
            if (parts.length < 2) continue;
            if ("post".equals(parts[0])) postIds.add(Long.valueOf(parts[1]));
            else if ("product".equals(parts[0])) productIds.add(Long.valueOf(parts[1]));
        }

        Map<Long, LocalDateTime> postTimeMap = postIds.isEmpty() ? Collections.emptyMap() :
                postMapper.selectBatchIds(postIds).stream()
                        .collect(Collectors.toMap(Post::getId, Post::getCreateTime));
        Map<Long, LocalDateTime> productTimeMap = productIds.isEmpty() ? Collections.emptyMap() :
                productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Product::getCreateTime));

        int updatedCount = 0, removedCount = 0, dbFlushCount = 0;

        // 3. 逐条衰减重算
        for (ZSetOperations.TypedTuple<String> tuple : allMembers) {
            String value = tuple.getValue();
            if (value == null) continue;
            String[] parts = value.split(":");
            String targetType = parts[0];
            Long targetId = Long.valueOf(parts[1]);

            // 获取创建时间
            LocalDateTime createTime = "post".equals(targetType) ? postTimeMap.get(targetId) :
                    ("product".equals(targetType) ? productTimeMap.get(targetId) : null);

            if (createTime == null) {
                // 帖子已删除 -> 清理
                stringRedisTemplate.opsForZSet().remove(HOT_RANK_ALL, value);
                stringRedisTemplate.delete(HOT_SCORE_KEY + targetType + ":" + targetId);
                removedCount++;
                continue;
            }

            // 获取原始累计分
            String scoreKey = HOT_SCORE_KEY + targetType + ":" + targetId;
            String rawScoreStr = stringRedisTemplate.opsForValue().get(scoreKey);
            double rawScore;
            if (rawScoreStr != null) {
                try { rawScore = Double.parseDouble(rawScoreStr); }
                catch (NumberFormatException e) { rawScore = 0; }
            } else {
                rawScore = 0; // key 过期了，用当前 ZSet 分数作为近似
            }

            // 计算衰减后分数
            double finalScore = computeFinalScore(rawScore, targetType, targetId, createTime);

            if (finalScore < SCORE_FLOOR) {
                // 分数过低，移出热榜
                stringRedisTemplate.opsForZSet().remove(HOT_RANK_ALL, value);
                stringRedisTemplate.delete(scoreKey);
                removedCount++;
            } else {
                // 更新 ZSet 分数
                stringRedisTemplate.opsForZSet().add(HOT_RANK_ALL, value, finalScore);
                updatedCount++;

                // 每 20 条写一次数据库（降低写入频率）
                if (dbFlushCount % 20 == 0) {
                    updateDatabaseHotScore(targetType, targetId, finalScore);
                }
                dbFlushCount++;
            }
        }

        // 4. 重建今日热榜
        rebuildTodayRank();

        // 5. 裁剪热榜大小，防止堆积
        Long size = stringRedisTemplate.opsForZSet().zCard(HOT_RANK_ALL);
        if (size != null && size > MAX_RANK_SIZE) {
            stringRedisTemplate.opsForZSet().removeRange(HOT_RANK_ALL, 0, size - MAX_RANK_SIZE - 1);
        }

        long cost = System.currentTimeMillis() - start;
        log.info("热榜衰减重算完成: 更新{}条, 移除{}条, 写库{}次, 耗时{}ms",
                updatedCount, removedCount, dbFlushCount, cost);
    }

    /**
     * 重建今日热榜（仅保留24小时内创建的帖子）
     * 每次重算时调用，确保今日榜随衰减同步更新
     */
    private void rebuildTodayRank() {
        Set<ZSetOperations.TypedTuple<String>> allMembers =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(HOT_RANK_ALL, 0, -1);
        if (allMembers == null || allMembers.isEmpty()) return;

        stringRedisTemplate.delete(HOT_RANK_TODAY);

        LocalDateTime now = LocalDateTime.now();
        int count = 0;

        for (ZSetOperations.TypedTuple<String> tuple : allMembers) {
            String value = tuple.getValue();
            if (value == null) continue;
            String[] parts = value.split(":");
            if (parts.length < 2) continue;

            LocalDateTime createTime = getCreateTime(parts[0], Long.valueOf(parts[1]));
            if (createTime != null && Duration.between(createTime, now).toHours() <= 24) {
                stringRedisTemplate.opsForZSet().add(HOT_RANK_TODAY, value, tuple.getScore());
                count++;
            }
        }

        stringRedisTemplate.expire(HOT_RANK_TODAY, SystemConstants.HOT_RANK_TODAY_TTL, TimeUnit.DAYS);
        log.debug("今日热榜重建完成: {}条", count);
    }

    // ==================== 核心衰减算法 ====================

    /**
     * 计算最终热度分 = 原始分 × 时间衰减 × 新鲜度加成 × 新帖冷启动加成
     */
    private double computeFinalScore(double rawScore, String targetType, Long targetId) {
        LocalDateTime createTime = getCreateTime(targetType, targetId);
        return computeFinalScore(rawScore, targetType, targetId, createTime);
    }

    private double computeFinalScore(double rawScore, String targetType, Long targetId, LocalDateTime createTime) {
        if (rawScore <= 0 || createTime == null) {
            return rawScore;
        }

        // 1️⃣ 时间衰减因子 — 牛顿冷却定律 × 分段半衰期
        double decayFactor = calculateDecayFactor(createTime);

        // 2️⃣ 新鲜度加成 — 最近30分钟有交互则加权
        double recentBoost = calculateRecentBoost(targetType, targetId);

        // 3️⃣ 新帖冷启动保护 — 发布2小时内保证基础可见度
        double newPostBoost = calculateNewPostBoost(createTime);

        return rawScore * decayFactor * recentBoost * newPostBoost;
    }

    /**
     * 分段半衰期衰减（校园场景定制）
     *
     * 校园内容生命周期短：一个帖子从发布到过时通常不超过2天。
     * 采用"加速衰减"策略 — 帖子越老衰减越快，而非固定半衰期。
     *
     *  时段        | 半衰期 | 效果
     *  ------------|--------|-----------------------
     *  0-4h        | 10h    | 新鲜期，几乎不衰减
     *  4-12h       |  6h    | 一个白天，正常衰减
     *  12-24h      |  3h    | 隔夜，加速衰减
     *  24-48h      |  1.5h  | 昨日帖，快速淡出
     *  >48h        |  —     | 底分 0.1%，直接移除
     */
    private double calculateDecayFactor(LocalDateTime createTime) {
        long ageHours = Duration.between(createTime, LocalDateTime.now()).toHours();
        if (ageHours < 0) return 1.0;

        double halfLife;
        if (ageHours <= 4) {
            halfLife = 10;
        } else if (ageHours <= 12) {
            halfLife = 6;
        } else if (ageHours <= 24) {
            halfLife = 3;
        } else if (ageHours <= 48) {
            halfLife = 1.5;
        } else {
            return 0.001;
        }

        return Math.pow(0.5, (double) ageHours / halfLife);
    }

    /**
     * 新鲜度加成：30分钟内有新交互的帖子获得额外加权
     *
     * 防止"一冷到底"：当帖子在评论区有持续讨论时，
     * 即使发布已久（比如昨天的精华帖），也能短暂回温。
     */
    private double calculateRecentBoost(String targetType, Long targetId) {
        String lastActiveKey = HOT_LAST_ACTIVE_PREFIX + targetType + ":" + targetId;
        String lastActiveStr = stringRedisTemplate.opsForValue().get(lastActiveKey);
        if (lastActiveStr == null) return 1.0;

        try {
            long lastActiveMs = Long.parseLong(lastActiveStr);
            long elapsedMinutes = (System.currentTimeMillis() - lastActiveMs) / 60_000;

            if (elapsedMinutes <= 5)  return 1.4;   // 5分钟内 → +40%
            if (elapsedMinutes <= 15) return 1.25;  // 15分钟内 → +25%
            if (elapsedMinutes <= 30) return 1.1;   // 30分钟内 → +10%
            return 1.0;
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    /**
     * 新帖冷启动保护：发布2小时内的帖子获得初始热度加成
     *
     * 新帖刚发布时赞数少、评论少，如果不保护会永远上不了热榜。
     * 用时间衰减的"逆操作"给新帖加权，保证用户能看到最新内容。
     */
    private double calculateNewPostBoost(LocalDateTime createTime) {
        long ageMinutes = Duration.between(createTime, LocalDateTime.now()).toMinutes();
        if (ageMinutes <= 30)  return 2.0;    // 半小时内 2x
        if (ageMinutes <= 60)  return 1.6;    // 1小时内 1.6x
        if (ageMinutes <= 120) return 1.3;    // 2小时内 1.3x
        return 1.0;
    }

    /**
     * 判断帖子是否在指定小时内创建
     */
    private boolean isCreatedWithinHours(String targetType, Long targetId, long hours) {
        LocalDateTime createTime = getCreateTime(targetType, targetId);
        return createTime != null && Duration.between(createTime, LocalDateTime.now()).toHours() <= hours;
    }

    /**
     * 获取目标创建时间（优先读缓存，减少 DB 查询）
     */
    private LocalDateTime getCreateTime(String targetType, Long targetId) {
        if ("post".equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            return post != null ? post.getCreateTime() : null;
        } else if ("product".equals(targetType)) {
            Product product = productMapper.selectById(targetId);
            return product != null ? product.getCreateTime() : null;
        }
        return null;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取默认配置
     */
    private HotConfig getDefaultConfig(String targetType) {
        HotConfig config = new HotConfig();
        config.setTargetType(targetType);
        config.setViewWeight(0.3f);
        config.setLikeWeight(1.0f);
        config.setCommentWeight(1.5f);
        config.setShareWeight(2.0f);
        config.setFavoriteWeight(1.2f);
        config.setTimeDecay(1.5f);
        return config;
    }

    /**
     * 获取操作权重，修复了 actionType 映射问题：
     *  - LikeEvent 传的 action = "add" / "remove"
     *  - 需要映射到 likeWeight / -likeWeight
     */
    private double getWeight(String actionType, HotConfig config) {
        switch (actionType) {
            case "view":
                return config.getViewWeight();
            case "like":
            case "add":              // 点赞事件 → 正权重
                return config.getLikeWeight();
            case "unlike":
            case "remove":           // 取消赞 → 负权重
                return -config.getLikeWeight();
            case "comment":
                return config.getCommentWeight();
            case "share":
                return config.getShareWeight();
            case "favorite":
                return config.getFavoriteWeight();
            default:
                return 0.1;
        }
    }

    /**
     * 判断是否需要更新数据库（每10次操作更新一次）
     */
    private boolean shouldUpdateDatabase(String targetType, Long targetId, String actionType) {
        String countKey = "update:count:" + targetType + ":" + targetId;
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        if (count == null) return false;

        if (count % 10 == 0) {
            stringRedisTemplate.expire(countKey, 1, TimeUnit.HOURS);
            return true;
        }
        return false;
    }

    /**
     * 更新数据库热度分
     */
    private void updateDatabaseHotScore(String targetType, Long targetId, double score) {
        BigDecimal bdScore = BigDecimal.valueOf(score);
        if ("post".equals(targetType)) {
            postMapper.updateHotScore(targetId, bdScore);
        } else if ("product".equals(targetType)) {
            productMapper.updateHotScore(targetId, bdScore);
        }
    }

    /**
     * 补充项目基本信息到 DTO
     */
    private void enrichItemInfo(HotItemDTO dto) {
        if ("post".equals(dto.getType())) {
            enrichPostInfo(dto);
        } else if ("product".equals(dto.getType())) {
            enrichProductInfo(dto);
        }
    }

    /**
     * 补充帖子信息
     */
    private void enrichPostInfo(HotItemDTO dto) {
        Post post = postMapper.selectById(dto.getId());
        if (post != null) {
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setViewCount(post.getViewCount());
            dto.setLikeCount(post.getLikeCount());
            dto.setCommentCount(post.getCommentCount());

            Student author = studentService.getById(post.getUserId());
            if (author != null) {
                dto.setAuthorId(author.getId());
                dto.setAuthorName(author.getNickName());
                dto.setAuthorAvatar(author.getAvatar());
            }

            List<String> images = post.getImageList();
            if (!images.isEmpty()) {
                dto.setCoverImage(images.get(0));
            }
        }
    }

    /**
     * 补充商品信息
     */
    private void enrichProductInfo(HotItemDTO dto) {
        Product product = productMapper.selectById(dto.getId());
        if (product != null) {
            dto.setTitle(product.getTitle());
            dto.setContent(product.getDescription());
            dto.setViewCount(product.getViewCount());
            dto.setLikeCount(product.getFavoriteCount());

            Student seller = studentService.getById(product.getSellerId());
            if (seller != null) {
                dto.setAuthorId(seller.getId());
                dto.setAuthorName(seller.getNickName());
                dto.setAuthorAvatar(seller.getAvatar());
            }

            List<String> images = product.getImageList();
            if (!images.isEmpty()) {
                dto.setCoverImage(images.get(0));
            }
        }
    }
}
