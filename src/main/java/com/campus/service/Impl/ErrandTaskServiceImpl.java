package com.campus.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.Result;
import com.campus.entity.ErrandOrder;
import com.campus.entity.ErrandTask;
import com.campus.entity.Student;
import com.campus.mapper.ErrandOrderMapper;
import com.campus.mapper.ErrandTaskMapper;
import com.campus.service.ContentModerationService;
import com.campus.service.IErrandTaskService;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ErrandTaskServiceImpl extends ServiceImpl<ErrandTaskMapper, ErrandTask>
        implements IErrandTaskService {

    @Resource
    private IStudentService studentService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ErrandOrderMapper errandOrderMapper;  // 直接注入订单Mapper，避免循环依赖

    @Resource
    private ContentModerationService contentModerationService;

    @Override
    @Transactional
    public Result publishTask(ErrandTask task) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 内容安全审核
        ContentModerationService.ModerationResult titleCheck = contentModerationService.moderate(task.getTitle(), true);
        if (!titleCheck.isPassed()) {
            return Result.fail("标题包含违规内容，请修改");
        }
        ContentModerationService.ModerationResult descCheck = contentModerationService.moderate(task.getDescription(), false);
        if (!descCheck.isPassed()) {
            return Result.fail("描述包含违规内容，请修改");
        }
        task.setDescription(descCheck.getCleanedContent());

        // 校验酬金
        if (task.getReward() == null || task.getReward().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("酬金必须大于0");
        }

        // 校验截止时间
        if (task.getDeadline() != null && task.getDeadline().isBefore(LocalDateTime.now())) {
            return Result.fail("截止时间不能早于当前时间");
        }

        task.setPublisherId(userId);
        task.setStatus(1); // 待接单
        task.setCreateTime(LocalDateTime.now());

        boolean success = save(task);
        if (!success) {
            return Result.fail("发布任务失败");
        }

        log.info("用户{}发布跑腿任务：{}", userId, task.getId());
        return Result.ok(task.getId());
    }

    @Override
    public Result getTaskDetail(Long taskId) {
        ErrandTask task = getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 查询发布者信息
        Student publisher = studentService.getById(task.getPublisherId());
        if (publisher != null) {
            task.setPublisherName(publisher.getNickName());
            task.setPublisherAvatar(publisher.getAvatar());
        }

        // 如果有接单者，查询接单者信息
        if (task.getAcceptorId() != null) {
            Student acceptor = studentService.getById(task.getAcceptorId());
            if (acceptor != null) {
                task.setAcceptorName(acceptor.getNickName());
            }
        }

        return Result.ok(task);
    }

    @Override
    public Result getNearbyTasks(Double latitude, Double longitude, Integer current) {
        if (latitude == null || longitude == null) {
            // 没有位置信息，返回最新任务
            Page<ErrandTask> page = lambdaQuery()
                    .eq(ErrandTask::getStatus, 1)
                    .orderByDesc(ErrandTask::getCreateTime)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords(), page.getTotal());
        }

        // 查询附近5公里内的任务
        List<ErrandTask> tasks = baseMapper.selectNearbyTasks(latitude, longitude, 5.0);

        // 分页处理
        int start = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = Math.min(start + SystemConstants.DEFAULT_PAGE_SIZE, tasks.size());

        if (start >= tasks.size()) {
            return Result.ok(List.of());
        }

        return Result.ok(tasks.subList(start, end));
    }

    @Override
    @Transactional
    public Result acceptTask(Long taskId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        String lockKey = "errand:lock:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 获取分布式锁，防止重复接单
            boolean isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (!isLock) {
                return Result.fail("操作太频繁，请稍后再试");
            }

            // 1. 查询任务
            ErrandTask task = getById(taskId);
            if (task == null) {
                return Result.fail("任务不存在");
            }

            // 2. 校验任务状态
            if (task.getStatus() != 1) {
                return Result.fail("任务已被接单或已取消");
            }

            // 3. 不能接自己发布的任务
            if (task.getPublisherId().equals(userId)) {
                return Result.fail("不能接自己发布的任务");
            }

            // 4. 更新任务状态（原子操作）
            int rows = baseMapper.acceptTask(taskId, userId, 2);
            if (rows == 0) {
                return Result.fail("接单失败，可能已被抢单");
            }

            // 5. 创建订单
            ErrandOrder order = createOrder(task, userId);

            log.info("用户{}接单成功，任务ID：{}，订单ID：{}", userId, taskId, order.getId());

            return Result.ok(order.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("接单失败", e);
            return Result.fail("接单失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 创建订单
     */
    private ErrandOrder createOrder(ErrandTask task, Long acceptorId) {
        ErrandOrder order = new ErrandOrder();
        order.setTaskId(task.getId());
        order.setPublisherId(task.getPublisherId());
        order.setAcceptorId(acceptorId);
        order.setReward(task.getReward());
        order.setStatus(1);      // 1-待支付
        order.setPayStatus(0);   // 0-未支付
        order.setCreateTime(LocalDateTime.now());

        errandOrderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional
    public Result completeTask(Long taskId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 查询任务
        ErrandTask task = getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 2. 只有接单者才能完成任务
        if (task.getAcceptorId() == null || !task.getAcceptorId().equals(userId)) {
            return Result.fail("无权操作");
        }

        // 3. 校验任务状态
        if (task.getStatus() != 2 && task.getStatus() != 3) {
            return Result.fail("任务状态不正确");
        }

        // 4. 更新任务状态
        task.setStatus(3); // 进行中
        task.setFinishTime(LocalDateTime.now());
        boolean success = updateById(task);

        if (success) {
            // 5. 更新订单状态
            ErrandOrder order = errandOrderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErrandOrder>()
                            .eq(ErrandOrder::getTaskId, taskId)
            );
            if (order != null) {
                order.setStatus(2); // 已支付
                errandOrderMapper.updateById(order);
            }
            log.info("用户{}完成任务：{}", userId, taskId);
        }

        return success ? Result.ok() : Result.fail("操作失败");
    }

    @Override
    @Transactional
    public Result cancelTask(Long taskId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 查询任务
        ErrandTask task = getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 2. 只有发布者才能取消
        if (!task.getPublisherId().equals(userId)) {
            return Result.fail("无权操作");
        }

        // 3. 只有待接单状态才能取消
        if (task.getStatus() != 1) {
            return Result.fail("任务已被接单，无法取消");
        }

        // 4. 更新任务状态
        task.setStatus(5); // 已取消
        boolean success = updateById(task);

        if (success) {
            log.info("用户{}取消任务：{}", userId, taskId);
        }

        return success ? Result.ok() : Result.fail("取消失败");
    }

    @Override
    public Result getMyPublishedTasks(Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Page<ErrandTask> page = lambdaQuery()
                .eq(ErrandTask::getPublisherId, userId)
                .orderByDesc(ErrandTask::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        return Result.ok(page.getRecords(), page.getTotal());
    }

    @Override
    public Result getMyAcceptedTasks(Integer current) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Page<ErrandTask> page = lambdaQuery()
                .eq(ErrandTask::getAcceptorId, userId)
                .orderByDesc(ErrandTask::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        return Result.ok(page.getRecords(), page.getTotal());
    }
}