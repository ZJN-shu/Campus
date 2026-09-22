package com.campus.controller;

import com.campus.cache.MultiLevelCache;
import com.campus.dto.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Resource
    private MultiLevelCache multiLevelCache;

    /**
     * 查看本地缓存命中率
     */
    @GetMapping("/cache/hit-rate")
    public Result getCacheHitRate() {
        return Result.ok(multiLevelCache.getLocalHitRate());
    }

    /**
     * 查看本地缓存统计
     */
    @GetMapping("/cache/stats")
    public Result getCacheStats() {
        return Result.ok(multiLevelCache.getLocalStats());
    }

    /**
     * 手动清除缓存
     */
    @DeleteMapping("/cache/{key}")
    public Result evictCache(@PathVariable String key) {
        multiLevelCache.evict(key);
        return Result.ok("清除成功");
    }
}