package com.campus.controller;

import com.campus.dto.Result;
import com.campus.service.IMessageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private IMessageService messageService;

    /**
     * 获取我的消息列表
     */
    @GetMapping("/list")
    public Result getMessages(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(required = false) Integer type) {
        return messageService.getMyMessages(current, type);
    }

    /**
     * 标记消息已读
     */
    @PutMapping("/{id}/read")
    public Result markAsRead(@PathVariable Long id) {
        return messageService.markAsRead(id);
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    public Result markAllAsRead() {
        return messageService.markAllAsRead();
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public Result getUnreadCount() {
        return messageService.getUnreadCount();
    }
}
