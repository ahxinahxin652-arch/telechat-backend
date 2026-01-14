package com.telechat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

@Service
@Slf4j
public class MessageService {
    
    @Autowired
    private TelechatWebSocketHandler webSocketHandler;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, String type, Object content) {
        try {
            TelechatWebSocketHandler.WebSocketMessage message = new TelechatWebSocketHandler.WebSocketMessage();
            message.setType(type);
            // 这里需要根据content的实际类型进行处理
            message.setContent(objectMapper.writeValueAsString(content));
            String jsonMessage = objectMapper.writeValueAsString(message);
            // 修复：检查webSocketHandler是否为null
            if (webSocketHandler != null) {
                webSocketHandler.sendMessageToUser(userId, new TextMessage(jsonMessage));
            }
        } catch (Exception e) {
            log.error("发送消息给用户 {} 失败: {}", userId, e.getMessage());
        }
    }
}