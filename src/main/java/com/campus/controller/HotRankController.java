package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.IHotRankService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hot")
public class HotRankController {

    @Resource
    private IHotRankService hotRankService;

    /**
     * 获取热榜
     */
    @GetMapping("/rank")
    public Result getHotRank(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "1") Integer current) {
        return hotRankService.getHotRank(category, current);
    }

    /**
     * 获取24小时热榜
     */
    @GetMapping("/today")
    public Result getTodayHot() {
        return hotRankService.getTodayHot();
    }

    /**
     * 获取上升最快榜
     */
    @GetMapping("/rising")
    public Result getRisingHot() {
        return hotRankService.getRisingHot();
    }
}