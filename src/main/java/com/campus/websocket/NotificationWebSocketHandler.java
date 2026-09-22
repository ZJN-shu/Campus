package com.campus.websocket;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知 WebSocket 处理器
 * 维护 userId → WebSocketSession 映射，支持向指定用户推送消息
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    /** 在线用户会话：userId → session */
    private static final Map<Long, WebSocketSession> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId != null) {
            ONLINE_SESSIONS.put(userId, session);
            log.info("WebSocket 连接建立: userId={}", userId);
            // 发送欢迎消息
            sendToUser(userId, Map.of("type", "connected", "message", "连接成功"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端发来的消息（如心跳 ping）
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.error("WebSocket pong 发送失败", e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUserId(session);
        if (userId != null) {
            ONLINE_SESSIONS.remove(userId);
            log.info("WebSocket 连接关闭: userId={}, status={}", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = resolveUserId(session);
        log.warn("WebSocket 传输错误: userId={}", userId, exception);
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭 WebSocket 会话失败", e);
            }
        }
        if (userId != null) {
            ONLINE_SESSIONS.remove(userId);
        }
    }

    // ==================== 推送方法 ====================

    /**
     * 向指定用户推送消息
     */
    public boolean sendToUser(Long userId, Object data) {
        WebSocketSession session = ONLINE_SESSIONS.get(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            String json = JSON.toJSONString(data);
            session.sendMessage(new TextMessage(json));
            return true;
        } catch (IOException e) {
            log.error("WebSocket 消息发送失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(Object data) {
        String json = JSON.toJSONString(data);
        ONLINE_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.error("广播消息失败: userId={}", userId, e);
                }
            }
        });
    }

    /**
     * 获取当前在线用户数
     */
    public int getOnlineCount() {
        return ONLINE_SESSIONS.size();
    }

    /**
     * 判断用户是否在线
     */
    public boolean isOnline(Long userId) {
        WebSocketSession session = ONLINE_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 从 session attributes 中解析 userId
     */
    private Long resolveUserId(WebSocketSession session) {
        // 优先从 attributes 中取（握手拦截器存入的 token 解析出的 userId）
        Object userIdAttr = session.getAttributes().get("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }
        // 尝试从 token 解析
        Object tokenAttr = session.getAttributes().get("token");
        if (tokenAttr instanceof String token) {
            String key = "login:token:" + token;
            Object idObj = stringRedisTemplate.opsForHash().get(key, "id");
            if (idObj != null) {
                Long userId = Long.valueOf(idObj.toString());
                session.getAttributes().put("userId", userId);
                return userId;
            }
        }
        return null;
    }
}
