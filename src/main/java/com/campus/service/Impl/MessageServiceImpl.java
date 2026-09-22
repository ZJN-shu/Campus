package com.campus.service.Impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.dto.MessageDTO;
import com.campus.dto.Result;
import com.campus.entity.Message;
import com.campus.entity.Post;
import com.campus.entity.Product;
import com.campus.mapper.MessageMapper;
import com.campus.service.IMessageService;
import com.campus.service.IPostService;
import com.campus.service.IProductService;
import com.campus.service.IStudentService;
import com.campus.utils.StudentHolder;
import com.campus.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements IMessageService {

    @Resource
    private IPostService postService;

    @Resource
    private IProductService productService;

    @Resource
    private IStudentService studentService;

    @Override
    public Result getMyMessages(Integer current, Integer type) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        int offset = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int limit = SystemConstants.DEFAULT_PAGE_SIZE;

        // 查询消息列表
        List<Message> messages = baseMapper.selectMessagesWithUser(
                userId, type, offset, limit);

        // 转换为DTO
        List<MessageDTO> dtoList = messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Result.ok(dtoList);
    }

    @Override
    @Transactional
    public Result markAsRead(Long messageId) {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        Message message = getById(messageId);
        if (message == null) {
            return Result.fail("消息不存在");
        }

        // 只能标记自己的消息
        if (!message.getToUserId().equals(userId)) {
            return Result.fail("无权操作");
        }

        message.setIsRead(1);
        boolean success = updateById(message);

        return success ? Result.ok() : Result.fail("操作失败");
    }

    @Override
    @Transactional
    public Result markAllAsRead() {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        baseMapper.markAllAsRead(userId);
        return Result.ok();
    }

    @Override
    public Result getUnreadCount() {
        Long userId = StudentHolder.getStudentId();
        if (userId == null) {
            return Result.fail("请先登录");
        }

        int count = baseMapper.getUnreadCount(userId);
        return Result.ok(count);
    }

    @Override
    public void sendSystemMessage(Long toUserId, String content, String targetType, Long targetId) {
        Message message = new Message();
        message.setFromUserId(0L); // 0表示系统
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setType(5); // 系统消息
        message.setTargetType(targetType);
        message.setTargetId(targetId);
        message.setIsRead(0);

        save(message);
    }

    /**
     * 发送互动消息（评论、点赞、收藏、关注）
     */
    @Transactional
    public void sendInteractionMessage(Long fromUserId, Long toUserId,
                                       Integer type, String targetType, Long targetId) {
        if (fromUserId.equals(toUserId)) {
            return; // 自己给自己不发消息
        }

        String content = generateMessageContent(type, targetType, targetId);

        Message message = new Message();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setType(type);
        message.setTargetType(targetType);
        message.setTargetId(targetId);
        message.setIsRead(0);

        save(message);
    }

    /**
     * 生成消息内容
     */
    private String generateMessageContent(Integer type, String targetType, Long targetId) {
        String targetName = "";
        if ("post".equals(targetType)) {
            Post post = postService.getById(targetId);
            targetName = post != null ? post.getTitle() : "帖子";
        } else if ("product".equals(targetType)) {
            Product product = productService.getById(targetId);
            targetName = product != null ? product.getTitle() : "商品";
        }

        if (targetName.length() > 20) {
            targetName = targetName.substring(0, 20) + "...";
        }

        switch (type) {
            case 1: return "评论了你的帖子：" + targetName;
            case 2: return "点赞了你的帖子：" + targetName;
            case 3: return "收藏了你的商品：" + targetName;
            case 4: return "关注了你";
            default: return "你有新的消息";
        }
    }

    /**
     * 转换为DTO
     */
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = BeanUtil.copyProperties(message, MessageDTO.class);
        dto.setIsRead(message.getIsRead() == 1);

        // 查询目标标题
        if (message.getTargetId() != null) {
            if ("post".equals(message.getTargetType())) {
                Post post = postService.getById(message.getTargetId());
                if (post != null) {
                    dto.setTargetTitle(post.getTitle());
                }
            } else if ("product".equals(message.getTargetType())) {
                Product product = productService.getById(message.getTargetId());
                if (product != null) {
                    dto.setTargetTitle(product.getTitle());
                }
            }
        }

        return dto;
    }
}