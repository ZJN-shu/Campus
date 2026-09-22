package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.Result;
import com.campus.entity.Follow;
import com.campus.entity.Student;
import com.campus.mapper.FollowMapper;
import com.campus.service.IFollowService;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.campus.utils.RedisConstants.FOLLOW_KEY;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
        implements IFollowService {

    @Resource
    private FollowMapper followMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IStudentService studentService;

    @Override
    @Transactional
    public Result follow(Long followeeId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }
        if (userId.equals(followeeId)) {
            return Result.fail("不能关注自己");
        }

        // 检查是否已关注
        Long count = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFolloweeId, followeeId));
        if (count > 0) {
            return Result.fail("已经关注过了");
        }

        // 保存关注
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFolloweeId(followeeId);
        followMapper.insert(follow);

        // Redis 记录
        stringRedisTemplate.opsForSet().add(FOLLOW_KEY + userId, followeeId.toString());

        return Result.ok();
    }

    @Override
    @Transactional
    public Result unfollow(Long followeeId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFolloweeId, followeeId));

        stringRedisTemplate.opsForSet().remove(FOLLOW_KEY + userId, followeeId.toString());

        return Result.ok();
    }

    @Override
    public Boolean isFollowed(Long followeeId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return false;
        }
        String key = FOLLOW_KEY + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, followeeId.toString()));
    }

    @Override
    public Result getFollowers(Long userId, Integer current) {
        Page<Follow> page = new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, userId)
                .orderByDesc(Follow::getCreateTime);
        Page<Follow> result = followMapper.selectPage(page, wrapper);

        List<Student> followers = result.getRecords().stream()
                .map(f -> studentService.getById(f.getUserId()))
                .filter(s -> s != null)
                .collect(Collectors.toList());

        return Result.ok(followers, result.getTotal());
    }

    @Override
    public Result getFollowees(Long userId, Integer current) {
        Page<Follow> page = new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE);
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId)
                .orderByDesc(Follow::getCreateTime);
        Page<Follow> result = followMapper.selectPage(page, wrapper);

        List<Student> followees = result.getRecords().stream()
                .map(f -> studentService.getById(f.getFolloweeId()))
                .filter(s -> s != null)
                .collect(Collectors.toList());

        return Result.ok(followees, result.getTotal());
    }

    @Override
    public Result getCommonFollows(Long userId) {
        Long currentUserId = StudentHolder.getStudentId();
        if (currentUserId == null) {
            return Result.fail("请先登录");
        }

        List<Long> commonIds = followMapper.selectCommonFollows(currentUserId, userId);

        List<Student> commonUsers = commonIds.stream()
                .map(id -> studentService.getById(id))
                .filter(s -> s != null)
                .collect(Collectors.toList());

        return Result.ok(commonUsers);
    }
}
