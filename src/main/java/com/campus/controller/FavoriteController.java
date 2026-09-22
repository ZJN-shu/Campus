package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.IFavoriteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Resource
    private IFavoriteService favoriteService;

    /**
     * 收藏
     */
    @PostMapping
    public Result favorite(@RequestBody Map<String, Object> params) {
        String targetType = (String) params.get("targetType");
        Long targetId = Long.valueOf(params.get("targetId").toString());
        return favoriteService.favorite(targetType, targetId);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping
    public Result unfavorite(@RequestBody Map<String, Object> params) {
        String targetType = (String) params.get("targetType");
        Long targetId = Long.valueOf(params.get("targetId").toString());
        return favoriteService.unfavorite(targetType, targetId);
    }

    /**
     * 是否已收藏
     */
    @GetMapping("/check")
    public Result isFavorited(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return Result.ok(favoriteService.isFavorited(targetType, targetId));
    }

    /**
     * 我的收藏列表
     */
    @GetMapping("/my")
    public Result getMyFavorites(
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") Integer current) {
        return favoriteService.queryMyFavorites(targetType, current);
    }
}
