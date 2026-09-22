package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.IFollowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注
     */
    @PostMapping
    public Result follow(@RequestBody Map<String, Object> params) {
        Long followeeId = Long.valueOf(params.get("followeeId").toString());
        return followService.follow(followeeId);
    }

    /**
     * 取消关注
     */
    @DeleteMapping
    public Result unfollow(@RequestBody Map<String, Object> params) {
        Long followeeId = Long.valueOf(params.get("followeeId").toString());
        return followService.unfollow(followeeId);
    }

    /**
     * 是否已关注
     */
    @GetMapping("/check")
    public Result isFollowed(@RequestParam Long followeeId) {
        return Result.ok(followService.isFollowed(followeeId));
    }

    /**
     * 粉丝列表
     */
    @GetMapping("/followers/{userId}")
    public Result getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer current) {
        return followService.getFollowers(userId, current);
    }

    /**
     * 关注列表
     */
    @GetMapping("/followees/{userId}")
    public Result getFollowees(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer current) {
        return followService.getFollowees(userId, current);
    }

    /**
     * 共同关注
     */
    @GetMapping("/common/{userId}")
    public Result getCommonFollows(@PathVariable Long userId) {
        return followService.getCommonFollows(userId);
    }
}
