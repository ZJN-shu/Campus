package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.Result;
import com.campus.entity.ErrandOrder;
import com.campus.entity.ErrandTask;
import com.campus.entity.Student;
import com.campus.mapper.ErrandOrderMapper;
import com.campus.mapper.ErrandTaskMapper;
import com.campus.service.IErrandOrderService;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ErrandOrderServiceImpl extends ServiceImpl<ErrandOrderMapper, ErrandOrder>
        implements IErrandOrderService {

    @Resource
    private ErrandTaskMapper errandTaskMapper;

    @Resource
    private IStudentService studentService;

    @Override
    @Transactional
    public Result payOrder(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 查询订单
        ErrandOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 2. 只有发布者才能支付
        if (!order.getPublisherId().equals(userId)) {
            return Result.fail("无权支付此订单");
        }

        // 3. 检查订单状态
        if (order.getPayStatus() == 1) {
            return Result.fail("订单已支付");
        }

        if (order.getStatus() != 1) {
            return Result.fail("订单状态不正确");
        }

        // 4. 查询任务信息
        ErrandTask task = errandTaskMapper.selectById(order.getTaskId());
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 5. 校验任务状态
        if (task.getStatus() != 2) {
            return Result.fail("任务状态不正确，无法支付");
        }

        // 6. 模拟支付（实际项目中调用微信/支付宝SDK）
        boolean paySuccess = mockPayment(order.getReward());

        if (!paySuccess) {
            return Result.fail("支付失败，请重试");
        }

        // 7. 更新订单状态
        order.setPayStatus(1);
        order.setStatus(2); // 已支付
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        boolean updateSuccess = updateById(order);

        if (!updateSuccess) {
            return Result.fail("更新订单状态失败");
        }

        // 8. 更新任务状态为进行中
        task.setStatus(3); // 进行中
        errandTaskMapper.updateById(task);

        log.info("用户{}支付订单成功，订单ID：{}，金额：{}", userId, orderId, order.getReward());

        return Result.ok(order);
    }

    @Override
    public Result getOrderDetail(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 查询订单
        ErrandOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 2. 只有发布者或接单者才能查看
        if (!order.getPublisherId().equals(userId) && !order.getAcceptorId().equals(userId)) {
            return Result.fail("无权查看此订单");
        }

        // 3. 查询任务详情
        ErrandTask task = errandTaskMapper.selectById(order.getTaskId());

        // 4. 查询发布者信息
        Student publisher = studentService.getById(order.getPublisherId());

        // 5. 查询接单者信息
        Student acceptor = studentService.getById(order.getAcceptorId());

        // 6. 组装返回数据
        OrderDetailVO detail = new OrderDetailVO();
        detail.setOrder(order);
        detail.setTask(task);
        detail.setPublisher(publisher);
        detail.setAcceptor(acceptor);

        return Result.ok(detail);
    }

    @Override
    public Result getMyOrders(Integer current, Integer status) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ErrandOrder::getPublisherId, userId)
                .or()
                .eq(ErrandOrder::getAcceptorId, userId));

        if (status != null && status > 0) {
            wrapper.eq(ErrandOrder::getStatus, status);
        }

        wrapper.orderByDesc(ErrandOrder::getCreateTime);

        Page<ErrandOrder> page = page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE), wrapper);

        // 填充任务信息
        for (ErrandOrder order : page.getRecords()) {
            ErrandTask task = errandTaskMapper.selectById(order.getTaskId());
            if (task != null) {
                order.setTaskTitle(task.getTitle());
                order.setTaskStatus(task.getStatus());
            }
        }

        return Result.ok(page.getRecords(), page.getTotal());
    }

    @Override
    public Result getOrderByTaskId(Long taskId) {
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandOrder::getTaskId, taskId);
        ErrandOrder order = getOne(wrapper);

        if (order == null) {
            return Result.fail("订单不存在");
        }

        return Result.ok(order);
    }

    @Transactional
    @Override
    public Result refundOrder(Long orderId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        ErrandOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 只有发布者才能申请退款
        if (!order.getPublisherId().equals(userId)) {
            return Result.fail("无权操作");
        }

        // 只有已支付的订单才能退款
        if (order.getPayStatus() != 1) {
            return Result.fail("订单未支付，无法退款");
        }

        // 模拟退款
        boolean refundSuccess = true;

        if (refundSuccess) {
            order.setPayStatus(0);
            order.setStatus(3); // 已退款
            order.setUpdateTime(LocalDateTime.now());
            updateById(order);

            // 更新任务状态为已取消
            ErrandTask task = errandTaskMapper.selectById(order.getTaskId());
            if (task != null && task.getStatus() == 3) {
                task.setStatus(5); // 已取消
                errandTaskMapper.updateById(task);
            }

            log.info("用户{}退款成功，订单ID：{}", userId, orderId);
            return Result.ok("退款成功");
        }

        return Result.fail("退款失败");
    }

    /**
     * 模拟支付
     */
    private boolean mockPayment(BigDecimal amount) {
        // 实际项目中这里调用微信支付/支付宝SDK
        log.info("模拟支付，金额：{}", amount);
        return true;
    }

    /**
     * 订单详情VO
     */
    public static class OrderDetailVO {
        private ErrandOrder order;
        private ErrandTask task;
        private Student publisher;
        private Student acceptor;

        // getter/setter
        public ErrandOrder getOrder() { return order; }
        public void setOrder(ErrandOrder order) { this.order = order; }

        public ErrandTask getTask() { return task; }
        public void setTask(ErrandTask task) { this.task = task; }

        public Student getPublisher() { return publisher; }
        public void setPublisher(Student publisher) { this.publisher = publisher; }

        public Student getAcceptor() { return acceptor; }
        public void setAcceptor(Student acceptor) { this.acceptor = acceptor; }
    }
}