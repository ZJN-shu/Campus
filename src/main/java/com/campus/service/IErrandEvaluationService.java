package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.ErrandEvaluation;

public interface IErrandEvaluationService extends IService<ErrandEvaluation> {

    Result evaluate(Long taskId, Integer score, String content);

    Result getUserEvaluations(Long userId, Integer current);

    Result getTaskEvaluation(Long taskId);

    Result getUserCreditScore(Long userId);
}