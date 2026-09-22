package com.campus.service;

import com.campus.utils.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * 内容安全审核服务
 * 策略：轻度违规自动替换，严重违规直接拒绝
 */
@Slf4j
@Service
public class ContentModerationService {

    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    @Data
    @AllArgsConstructor
    public static class ModerationResult {
        private boolean passed;
        private String cleanedContent;
        private Set<String> hitWords;

        public static ModerationResult pass(String content) {
            return new ModerationResult(true, content, Collections.emptySet());
        }
        public static ModerationResult reject(Set<String> hitWords) {
            return new ModerationResult(false, null, hitWords);
        }
        public static ModerationResult cleaned(String cleaned, Set<String> hitWords) {
            return new ModerationResult(true, cleaned, hitWords);
        }
    }

    /**
     * 审核文本内容
     * @param text 待审核文本
     * @param strict 是否严格模式（true=直接拒绝，false=自动替换）
     */
    public ModerationResult moderate(String text, boolean strict) {
        if (text == null || text.isEmpty()) {
            return ModerationResult.pass(text);
        }
        Set<String> hitWords = sensitiveWordFilter.findSensitiveWords(text);
        if (hitWords.isEmpty()) {
            return ModerationResult.pass(text);
        }
        log.warn("内容审核命中敏感词: {}", hitWords);
        if (strict) {
            return ModerationResult.reject(hitWords);
        }
        String cleaned = sensitiveWordFilter.replaceSensitiveWords(text);
        return ModerationResult.cleaned(cleaned, hitWords);
    }

    /** 快速检测是否包含敏感词 */
    public boolean hasSensitiveWord(String text) {
        return sensitiveWordFilter.containsSensitiveWord(text);
    }

    /** 自动替换敏感词 */
    public String cleanContent(String text) {
        return sensitiveWordFilter.replaceSensitiveWords(text);
    }
}
