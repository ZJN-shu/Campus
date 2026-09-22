package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.IErrandEvaluationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/errand/evaluation")
public class ErrandEvaluationController {

    @Resource
    private IErrandEvaluationService evaluationService;

    /**
     * 评价
     */
    @PostMapping
    public Result evaluate(
            @RequestParam Long taskId,
            @RequestParam Integer score,
            @RequestParam(required = false) String content) {
        return evaluationService.evaluate(taskId, score, content);
    }

    /**
     * 用户评价列表
     */
    @GetMapping("/user/{userId}")
    public Result getUserEvaluations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer current) {
        return evaluationService.getUserEvaluations(userId, current);
    }
}