package com.campus.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 基于 DFA（Trie 树）的敏感词过滤器
 * 支持跳过特殊字符（如 *、空格、符号），防止绕过
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    private final Map<Character, TrieNode> trieRoot = new HashMap<>();
    private static final Set<Character> SKIP_CHARS = new HashSet<>(
            Arrays.asList(' ', '\t', '\n', '\r', '*', '#', '$', '@', '!', ',', '.', ';', ':', '"', '\'')
    );

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
    }

    @PostConstruct
    public void init() {
        loadWords();
    }

    private void loadWords() {
        int count = 0;
        try {
            ClassPathResource resource = new ClassPathResource("sensitive-words.txt");
            if (!resource.exists()) {
                log.warn("敏感词文件不存在: sensitive-words.txt");
                return;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        addToTrie(line.toLowerCase());
                        count++;
                    }
                }
            }
            log.info("敏感词库加载完成，共 {} 个词", count);
        } catch (Exception e) {
            log.error("加载敏感词库失败: {}", e.getMessage(), e);
        }
    }

    private void addToTrie(String word) {
        TrieNode current = trieRoot.computeIfAbsent(word.charAt(0), k -> new TrieNode());
        for (int i = 1; i < word.length(); i++) {
            char c = word.charAt(i);
            if (SKIP_CHARS.contains(c)) continue;
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        current.isEnd = true;
    }

    /** 检测文本是否包含敏感词 */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            int len = matchFrom(lower, i);
            if (len > 0) {
                log.debug("敏感词命中: '{}' 在位置 {}", text.substring(i, Math.min(i + len, text.length())), i);
                return true;
            }
        }
        return false;
    }

    /** 替换敏感词为 * 号 */
    public String replaceSensitiveWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String lower = text.toLowerCase();
        char[] chars = text.toCharArray();
        for (int i = 0; i < lower.length(); i++) {
            int len = matchFrom(lower, i);
            if (len > 0) {
                for (int j = i; j < i + len && j < chars.length; j++) {
                    if (chars[j] != ' ') chars[j] = '*';
                }
                i += len - 1;
            }
        }
        return new String(chars);
    }

    /** 获取文本中的敏感词集合 */
    public Set<String> findSensitiveWords(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) return found;
        String lower = text.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            int len = matchFrom(lower, i);
            if (len > 0) {
                found.add(text.substring(i, Math.min(i + len, text.length())));
                i += len - 1;
            }
        }
        return found;
    }

    private int matchFrom(String text, int start) {
        TrieNode node = trieRoot.get(text.charAt(start));
        if (node == null) return 0;
        int lastEnd = node.isEnd ? 1 : 0;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (SKIP_CHARS.contains(c)) continue;
            TrieNode next = node.children.get(c);
            if (next == null) break;
            if (next.isEnd) lastEnd = i - start + 1;
            node = next;
        }
        return lastEnd;
    }
}
