package com.campus.config;

import com.campus.websocket.NotificationWebSocketHandler;
import com.campus.websocket.WebSocketAuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private NotificationWebSocketHandler notificationHandler;

    @Resource
    private WebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationHandler, "/ws/notification")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}
