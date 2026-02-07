package com.telechat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telechat.pojo.dto.ws.WsMessage;
import com.telechat.pojo.enums.WsMessageType;
import com.telechat.util.SnowflakeIdGenerator; // [引用 1]
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelechatWebSocketHandler implements WebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    // 1. 注入雪花算法生成器 (保证高性能生成唯一ID)
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("用户 [{}] 上线，当前在线: {}", userId, userSessions.size());

            // 2. 发送连接成功通知 (系统消息也带上 ID)
            // 系统消息 senderId = 0
            WsMessage<String> systemMsg = WsMessage.of(
                    WsMessageType.SYSTEM,
                    snowflakeIdGenerator.nextId(),
                    0L,
                    "连接成功"
            );
            sendMsg(userId, systemMsg);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        Long userId = getUserId(session);
        if (userId == null) return;

        if (message instanceof PongMessage) return;
        if (!(message instanceof TextMessage)) return;

        String payload = ((TextMessage) message).getPayload();
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String typeStr = rootNode.path("type").asText();

            // 3. 处理不同类型的消息
            if (WsMessageType.TYPING.getValue().equals(typeStr)) {
                handleTypingMessage(userId, rootNode);
            }
            // 扩展：处理客户端发来的 ACK 确认消息 (企业级功能：消息必达)
            // else if ("ack".equals(typeStr)) {
            //     handleAckMessage(userId, rootNode);
            // }

        } catch (Exception e) {
            log.error("消息处理异常: ", e);
            // 错误消息也带上 ID
            sendMsg(userId, WsMessage.of(
                    WsMessageType.ERROR,
                    snowflakeIdGenerator.nextId(),
                    0L,
                    "消息格式错误"
            ));
        }
    }

    /**
     * 处理 "正在输入" 状态
     */
    private void handleTypingMessage(Long senderId, JsonNode rootNode) {
        JsonNode dataNode = rootNode.path("data");
        if (dataNode.isMissingNode()) return;

        long receiverId = dataNode.path("receiverId").asLong();

        // 4. 构造标准消息 (即便 "typing" 是瞬时状态，生成 ID 也有助于前端维护唯一的 key)
        WsMessage<Void> typingMsg = WsMessage.of(
                WsMessageType.TYPING,
                snowflakeIdGenerator.nextId(), // 生成唯一 ID
                senderId,
                null
        );

        sendMsg(receiverId, typingMsg);
    }

    /**
     * 【核心发送方法】
     * 保持不变，负责序列化和线程安全发送
     */
    public void sendMsg(Long receiverId, WsMessage<?> message) {
        WebSocketSession session = userSessions.get(receiverId);
        if (session == null || !session.isOpen()) {
            // 进阶：如果不是瞬时消息(typing)，而是业务消息(contact_apply)，
            //TODO 此时应考虑写入 Redis 离线消息队列
            return;
        }

        synchronized (session) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("发送消息给用户 [{}] 失败: {}", receiverId, e.getMessage());
                userSessions.remove(receiverId);
            }
        }
    }

    // ... handleTransportError, afterConnectionClosed, getUserId, removeSession 保持不变 ...
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        removeSession(session);
        log.error("连接异常", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        removeSession(session);
        log.info("用户 [{}] 下线", getUserId(session));
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private Long getUserId(WebSocketSession session) {
        Object id = session.getAttributes().get("userId");
        return id instanceof Long ? (Long) id : null;
    }

    private void removeSession(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
    }
}