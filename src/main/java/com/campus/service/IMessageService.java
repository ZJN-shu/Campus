package com.campus.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.dto.Result;
import com.campus.entity.Message;

public interface IMessageService extends IService<Message> {

    /**
     * 获取我的消息
     */
    Result getMyMessages(Integer current, Integer type);

    /**
     * 标记为已读
     */
    Result markAsRead(Long messageId);

    /**
     * 标记全部已读
     */
    Result markAllAsRead();

    /**
     * 获取未读消息数
     */
    Result getUnreadCount();

    /**
     * 发送系统消息
     */
    void sendSystemMessage(Long toUserId, String content, String targetType, Long targetId);
}