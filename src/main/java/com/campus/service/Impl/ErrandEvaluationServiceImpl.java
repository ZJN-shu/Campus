package com.campus.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.Result;
import com.campus.entity.ErrandEvaluation;
import com.campus.entity.ErrandOrder;
import com.campus.entity.ErrandTask;
import com.campus.entity.Student;
import com.campus.mapper.ErrandEvaluationMapper;
import com.campus.mapper.ErrandOrderMapper;
import com.campus.mapper.ErrandTaskMapper;
import com.campus.service.IErrandEvaluationService;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ErrandEvaluationServiceImpl extends ServiceImpl<ErrandEvaluationMapper, ErrandEvaluation>
        implements IErrandEvaluationService {

    @Resource
    private ErrandTaskMapper errandTaskMapper;

    @Resource
    private ErrandOrderMapper errandOrderMapper;

    @Resource
    private IStudentService studentService;

    @Override
    @Transactional
    public Result evaluate(Long taskId, Integer score, String content) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        // 1. 校验评分范围
        if (score < 1 || score > 5) {
            return Result.fail("评分必须在1-5之间");
        }

        // 2. 查询任务
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        // 3. 检查任务是否已完成
        if (task.getStatus() != 3 && task.getStatus() != 4) {
            return Result.fail("任务未完成，无法评价");
        }

        // 4. 确定评价对象
        Long toUserId;
        String evaluationType;

        if (task.getPublisherId().equals(userId)) {
            // 发布者评价接单者
            toUserId = task.getAcceptorId();
            evaluationType = "acceptor";
        } else if (task.getAcceptorId() != null && task.getAcceptorId().equals(userId)) {
            // 接单者评价发布者
            toUserId = task.getPublisherId();
            evaluationType = "publisher";
        } else {
            return Result.fail("无权评价此任务");
        }

        // 5. 检查是否已评价过
        long count = lambdaQuery()
                .eq(ErrandEvaluation::getTaskId, taskId)
                .eq(ErrandEvaluation::getFromUserId, userId)
                .count();

        if (count > 0) {
            return Result.fail("已经评价过了");
        }

        // 6. 保存评价
        ErrandEvaluation evaluation = new ErrandEvaluation();
        evaluation.setTaskId(taskId);
        evaluation.setFromUserId(userId);
        evaluation.setToUserId(toUserId);
        evaluation.setScore(score);
        evaluation.setContent(content);
        evaluation.setCreateTime(LocalDateTime.now());

        boolean success = save(evaluation);
        if (!success) {
            return Result.fail("评价失败");
        }

        // 7. 更新用户信用分
        updateCredit(toUserId, score);

        // 8. 更新订单评价状态
        updateOrderEvaluationStatus(taskId, userId);

        log.info("用户{}评价用户{}，任务ID：{}，评分：{}", userId, toUserId, taskId, score);

        return Result.ok(evaluation);
    }

    @Override
    public Result getUserEvaluations(Long userId, Integer current) {
        Page<ErrandEvaluation> page = lambdaQuery()
                .eq(ErrandEvaluation::getToUserId, userId)
                .orderByDesc(ErrandEvaluation::getCreateTime)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        // 填充评价人信息
        List<EvaluationVO> voList = page.getRecords().stream()
                .map(evaluation -> {
                    EvaluationVO vo = new EvaluationVO();
                    vo.setEvaluation(evaluation);

                    // 查询评价人信息
                    Student fromUser = studentService.getById(evaluation.getFromUserId());
                    if (fromUser != null) {
                        vo.setFromUserName(fromUser.getNickName());
                        vo.setFromUserAvatar(fromUser.getAvatar());
                    }

                    // 查询任务信息
                    ErrandTask task = errandTaskMapper.selectById(evaluation.getTaskId());
                    if (task != null) {
                        vo.setTaskTitle(task.getTitle());
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        return Result.ok(voList, page.getTotal());
    }

    @Override
    public Result getTaskEvaluation(Long taskId) {
        // 查询任务的所有评价
        List<ErrandEvaluation> evaluations = lambdaQuery()
                .eq(ErrandEvaluation::getTaskId, taskId)
                .orderByDesc(ErrandEvaluation::getCreateTime)
                .list();

        if (evaluations.isEmpty()) {
            return Result.ok(List.of());
        }

        // 计算平均分
        double avgScore = evaluations.stream()
                .mapToInt(ErrandEvaluation::getScore)
                .average()
                .orElse(0.0);

        // 填充评价人信息
        List<EvaluationVO> voList = evaluations.stream()
                .map(evaluation -> {
                    EvaluationVO vo = new EvaluationVO();
                    vo.setEvaluation(evaluation);

                    Student fromUser = studentService.getById(evaluation.getFromUserId());
                    if (fromUser != null) {
                        vo.setFromUserName(fromUser.getNickName());
                        vo.setFromUserAvatar(fromUser.getAvatar());
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        return Result.ok(voList);
    }

    @Override
    public Result getUserCreditScore(Long userId) {
        Student student = studentService.getById(userId);
        if (student == null) {
            return Result.fail("用户不存在");
        }

        // 计算好评率
        long total = lambdaQuery()
                .eq(ErrandEvaluation::getToUserId, userId)
                .count();

        long goodCount = lambdaQuery()
                .eq(ErrandEvaluation::getToUserId, userId)
                .ge(ErrandEvaluation::getScore, 4)
                .count();

        double goodRate = total == 0 ? 100.0 : (double) goodCount / total * 100;

        CreditInfoVO creditInfo = new CreditInfoVO();
        creditInfo.setCredit(student.getCredit());
        creditInfo.setTotalEvaluations((int) total);
        creditInfo.setGoodRate(goodRate);
        creditInfo.setGoodCount((int) goodCount);
        creditInfo.setBadCount((int) (total - goodCount));

        return Result.ok(creditInfo);
    }

    /**
     * 更新用户信用分
     */
    private void updateCredit(Long userId, Integer score) {
        // 根据评分增减信用分
        int delta;
        if (score >= 4) {
            delta = 2;   // 好评加2分
        } else if (score == 3) {
            delta = 0;   // 中评不加不减
        } else {
            delta = -5;  // 差评扣5分
        }

        studentService.lambdaUpdate()
                .eq(Student::getId, userId)
                .setSql("credit = credit + " + delta)
                .update();

        log.info("用户{}信用分{}", userId, delta >= 0 ? "增加" + delta : "减少" + (-delta));
    }

    /**
     * 更新订单评价状态
     */
    private void updateOrderEvaluationStatus(Long taskId, Long userId) {
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandOrder::getTaskId, taskId);
        ErrandOrder order = errandOrderMapper.selectOne(wrapper);

        if (order != null) {
            if (order.getPublisherId().equals(userId)) {
                order.setPublisherEvaluated(1);
            } else if (order.getAcceptorId().equals(userId)) {
                order.setAcceptorEvaluated(1);
            }

            // 如果双方都已评价，订单状态更新为已完成
            if (order.getPublisherEvaluated() == 1 && order.getAcceptorEvaluated() == 1) {
                order.setStatus(4); // 已完成

                // 更新任务状态
                ErrandTask task = errandTaskMapper.selectById(taskId);
                if (task != null && task.getStatus() == 3) {
                    task.setStatus(4); // 已完成
                    errandTaskMapper.updateById(task);
                }
            }

            errandOrderMapper.updateById(order);
        }
    }

    /**
     * 评价VO
     */
    public static class EvaluationVO {
        private ErrandEvaluation evaluation;
        private String fromUserName;
        private String fromUserAvatar;
        private String taskTitle;

        // getter/setter
        public ErrandEvaluation getEvaluation() { return evaluation; }
        public void setEvaluation(ErrandEvaluation evaluation) { this.evaluation = evaluation; }

        public String getFromUserName() { return fromUserName; }
        public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

        public String getFromUserAvatar() { return fromUserAvatar; }
        public void setFromUserAvatar(String fromUserAvatar) { this.fromUserAvatar = fromUserAvatar; }

        public String getTaskTitle() { return taskTitle; }
        public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    }

    /**
     * 信用分信息VO
     */
    public static class CreditInfoVO {
        private Integer credit;
        private Integer totalEvaluations;
        private Double goodRate;
        private Integer goodCount;
        private Integer badCount;

        // getter/setter
        public Integer getCredit() { return credit; }
        public void setCredit(Integer credit) { this.credit = credit; }

        public Integer getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(Integer totalEvaluations) { this.totalEvaluations = totalEvaluations; }

        public Double getGoodRate() { return goodRate; }
        public void setGoodRate(Double goodRate) { this.goodRate = goodRate; }

        public Integer getGoodCount() { return goodCount; }
        public void setGoodCount(Integer goodCount) { this.goodCount = goodCount; }

        public Integer getBadCount() { return badCount; }
        public void setBadCount(Integer badCount) { this.badCount = badCount; }
    }
}