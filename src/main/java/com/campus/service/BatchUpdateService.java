package com.campus.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.entity.Post;
import com.campus.entity.Product;
import com.campus.mapper.PostMapper;
import com.campus.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchUpdateService {

    private final PostMapper postMapper;
    private final ProductMapper productMapper;

    /**
     * 点赞计数器（内存缓存，定时刷入数据库）
     * key: targetType:targetId, value: 增量值
     */
    private final Map<String, LongAdder> likeCountBuffer = new ConcurrentHashMap<>();

    /**
     * 浏览计数器
     */
    private final Map<String, LongAdder> viewCountBuffer = new ConcurrentHashMap<>();

    /**
     * 添加点赞计数
     */
    public void addLikeCount(String targetType, Long targetId, int delta) {
        String key = targetType + ":" + targetId;
        likeCountBuffer.computeIfAbsent(key, k -> new LongAdder()).add(delta);
    }

    /**
     * 添加浏览计数
     */
    public void addViewCount(String targetType, Long targetId, int delta) {
        String key = targetType + ":" + targetId;
        viewCountBuffer.computeIfAbsent(key, k -> new LongAdder()).add(delta);
    }

    /**
     * 批量刷新点赞数到数据库
     */
    public void flushLikeCounts() {
        if (likeCountBuffer.isEmpty()) {
            return;
        }

        likeCountBuffer.forEach((key, adder) -> {
            long delta = adder.sumThenReset();
            if (delta == 0) return;

            String[] parts = key.split(":");
            String targetType = parts[0];
            Long targetId = Long.valueOf(parts[1]);

            try {
                if ("post".equals(targetType)) {
                    postMapper.update(null, new LambdaUpdateWrapper<Post>()
                            .eq(Post::getId, targetId)
                            .setSql("like_count = like_count + " + delta));
                } else if ("product".equals(targetType)) {
                    productMapper.update(null, new LambdaUpdateWrapper<Product>()
                            .eq(Product::getId, targetId)
                            .setSql("favorite_count = favorite_count + " + delta));
                }
                log.debug("批量更新点赞数: {}, 增量: {}", key, delta);
            } catch (Exception e) {
                log.error("批量更新点赞数失败: {}", key, e);
                // 失败时加回计数器
                likeCountBuffer.computeIfAbsent(key, k -> new LongAdder()).add(delta);
            }
        });
    }

    /**
     * 批量刷新浏览数
     */
    public void flushViewCounts() {
        if (viewCountBuffer.isEmpty()) {
            return;
        }

        viewCountBuffer.forEach((key, adder) -> {
            long delta = adder.sumThenReset();
            if (delta == 0) return;

            String[] parts = key.split(":");
            String targetType = parts[0];
            Long targetId = Long.valueOf(parts[1]);

            try {
                if ("post".equals(targetType)) {
                    postMapper.update(null, new LambdaUpdateWrapper<Post>()
                            .eq(Post::getId, targetId)
                            .setSql("view_count = view_count + " + delta));
                }
                log.debug("批量更新浏览数: {}, 增量: {}", key, delta);
            } catch (Exception e) {
                log.error("批量更新浏览数失败: {}", key, e);
                viewCountBuffer.computeIfAbsent(key, k -> new LongAdder()).add(delta);
            }
        });
    }

    /**
     * 定时刷新（由定时任务调用）
     */
    public void scheduledFlush() {
        flushLikeCounts();
        flushViewCounts();
    }
}