/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/8/22 下午11:34
 */
package com.telechat.config;

import com.telechat.websocket.TelechatHandshakeInterceptor;
import com.telechat.websocket.TelechatWebSocketHandler;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private TelechatWebSocketHandler webSocketHandler;

    @Resource
    private TelechatHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}