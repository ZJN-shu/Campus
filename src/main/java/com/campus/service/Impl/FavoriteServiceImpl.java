package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.Result;
import com.campus.entity.Favorite;
import com.campus.mapper.FavoriteMapper;
import com.campus.service.IFavoriteService;
import com.campus.service.IStatsService;  // 注入 StatsService
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.campus.utils.RedisConstants.*;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite>
        implements IFavoriteService {

    @Resource
    private FavoriteMapper favoriteMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IStatsService statsService;  // 使用 StatsService 而不是 IPostService

    @Override
    @Transactional
    public Result favorite(String targetType, Long targetId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 检查是否已收藏
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));

        if (count > 0) {
            return Result.fail("已经收藏过了");
        }

        // 2. 保存收藏记录
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType);
        favorite.setTargetId(targetId);
        favoriteMapper.insert(favorite);

        // 3. 更新目标收藏数 - 使用 StatsService
        if ("post".equals(targetType)) {
            statsService.incrementPostFavoriteCount(targetId);
        } else if ("product".equals(targetType)) {
            statsService.incrementProductFavoriteCount(targetId);
        }

        // 4. Redis记录
        String key = getFavoriteKey(targetType, targetId);
        stringRedisTemplate.opsForSet().add(key, userId.toString());

        return Result.ok();
    }

    @Override
    @Transactional
    public Result unfavorite(String targetType, Long targetId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 删除收藏记录
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));

        // 2. 更新目标收藏数 - 使用 StatsService
        if ("post".equals(targetType)) {
            statsService.decrementPostFavoriteCount(targetId);
        } else if ("product".equals(targetType)) {
            statsService.decrementProductFavoriteCount(targetId);
        }

        // 3. Redis移除
        String key = getFavoriteKey(targetType, targetId);
        stringRedisTemplate.opsForSet().remove(key, userId.toString());

        return Result.ok();
    }

    @Override
    public Boolean isFavorited(String targetType, Long targetId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return false;
        }

        String key = getFavoriteKey(targetType, targetId);
        Boolean isMember = stringRedisTemplate.opsForSet()
                .isMember(key, userId.toString());
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public Long getFavoriteCount(String targetType, Long targetId) {
        String key = getFavoriteKey(targetType, targetId);
        Long size = stringRedisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    @Override
    public Result queryMyFavorites(String targetType, Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Page<Favorite> page = new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE);
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId);

        if (targetType != null && !"all".equals(targetType)) {
            wrapper.eq(Favorite::getTargetType, targetType);
        }
        wrapper.orderByDesc(Favorite::getCreateTime);

        Page<Favorite> favoritePage = favoriteMapper.selectPage(page, wrapper);

        return Result.ok(favoritePage.getRecords(), favoritePage.getTotal());
    }

    private String getFavoriteKey(String targetType, Long targetId) {
        switch (targetType) {
            case "post":
                return FAVORITE_POST_KEY + targetId;
            case "product":
                return FAVORITE_PRODUCT_KEY + targetId;
            default:
                return "favorite:" + targetType + ":" + targetId;
        }
    }
}