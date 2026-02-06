package com.telechat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telechat.pojo.entity.ChatMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelechatWebSocketHandler implements WebSocketHandler {

    // 存储用户连接
    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();


    @Autowired
    private ObjectMapper objectMapper; // 正确序列化日期

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("用户 {} 连接建立，当前在线用户数: {}", userId, userSessions.size());

            // 发送连接成功消息
            WebSocketMessage connectMessage = new WebSocketMessage();
            connectMessage.setType("system");
            connectMessage.setContent("连接成功");
            connectMessage.setTimestamp(LocalDateTime.now());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectMessage)));
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) return;

        // 检查消息类型是否为TextMessage
        if (!(message instanceof TextMessage)) {
            log.warn("收到非文本消息类型: {}", message.getClass().getName());
            return;
        }

        // 获取文本消息内容
        String payload = ((TextMessage) message).getPayload();
        log.info("收到用户 {} 的消息: {}", userId, payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();

            switch (type) {
                case "chat":
                    //handleChatMessage(userId, jsonNode);
                    break;
                case "typing":
                    handleTypingMessage(userId, jsonNode);
                    break;
                default:
                    log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage());
        }
    }


    // 处理正在输入消息
    private void handleTypingMessage(Long senderId, JsonNode message) throws Exception {
        Long receiverId = message.get("receiverId").asLong();

        WebSocketSession receiverSession = userSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            WebSocketMessage typingMessage = new WebSocketMessage();
            typingMessage.setType("typing");
            typingMessage.setSenderId(senderId);
            typingMessage.setTimestamp(LocalDateTime.now());
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(typingMessage)));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            log.error("用户 {} 连接异常: {}", userId, exception.getMessage());
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            log.info("用户 {} 连接关闭，当前在线用户数: {}", userId, userSessions.size());
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    // 添加缺失的方法：发送消息给指定用户
    public void sendMessageToUser(Long userId, TextMessage message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (Exception e) {
                log.error("TEXT发送消息给用户 {} 失败: {}", userId, e.getMessage());
                // 如果发送失败，从会话列表中移除
                userSessions.remove(userId);
            }
        } else {
            log.warn("TEXT用户 {} 不在线或连接已关闭", userId);
        }
    }

    /**
     * 【核心修改点】发送消息给指定用户
     * 支持发送任意自定义对象 (DTO, VO, Map, etc.)
     * * @param userId 接收者ID
     * @param messagePayload 任意对象，将被 Jackson 序列化为 JSON
     */
    public void sendMessageToUser(Long userId, Object messagePayload) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            // 加锁防止多线程并发发送导致 "TEXT_PARTIAL_WRITING" 错误
            synchronized (session) {
                try {
                    // Jackson 会自动根据传入对象的类结构生成 JSON
                    String jsonMessage = objectMapper.writeValueAsString(messagePayload);
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    log.error("发送消息给用户 {} 失败", userId, e);
                }
            }
        } else {
            // 用户不在线的处理逻辑（例如存离线消息）
            log.debug("用户 {} 不在线，消息未发送", userId);
        }
    }

    // 内部消息类
    @Data
    public static class WebSocketMessage {
        private String type;
        private Long messageId;
        private Long senderId;
        private String content;
        private String status;
        private LocalDateTime timestamp;
    }
}