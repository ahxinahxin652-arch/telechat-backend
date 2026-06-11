package com.telechat.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telechat.websocket.message.WsMessage;
import com.telechat.pojo.enums.WsMessageType;
import com.telechat.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import com.telechat.service.UserDeviceSyncService;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class TelechatWebSocketHandler implements WebSocketHandler {

    // 存储用户ID和对应的Session，企业级IM中如果单机支持百万连接，通常建议结合 Netty 优化，但在万级/十万级并发内 Spring WebSocket 结合 ConcurrentHashMap 足够稳定
    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // 心跳超时容忍时间：90秒（前端心跳间隔45秒，允许丢失一次包，超过90秒无响应即认定掉线）
    private static final long SESSION_TIMEOUT_MS = 90_000L;

    // Session 属性中的最后活跃时间标识符
    private static final String LAST_ACTIVE_TIME_KEY = "lastActiveTime";

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private UserDeviceSyncService userDeviceSyncService;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            // 初始化/更新最后活跃时间
            updateSessionActiveTime(session);

            log.info("用户 [{}] 上线，当前在线人数: {}", userId, userSessions.size());

            WsMessage<String> systemMsg = WsMessage.of(
                    WsMessageType.SYSTEM,
                    snowflakeIdGenerator.nextId(),
                    0L,
                    System.currentTimeMillis(),
                    "连接成功"
            );
            sendMsg(userId, systemMsg);
        }
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message) {
        Long userId = getUserId(session);
        if (userId == null) return;

        // 【核心】只要收到该用户的任何消息，就认为他在线，刷新最后活跃时间
        updateSessionActiveTime(session);

        if (message instanceof PongMessage) return; // 忽略协议底层的Pong帧
        if (!(message instanceof TextMessage)) return;

        String payload = ((TextMessage) message).getPayload();

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String typeStr = rootNode.path("type").asText("");

            // 1. 处理前端发来的心跳包
            if (WsMessageType.PING.getValue().equals(typeStr)) {
                handlePingMessage(session, rootNode);
                log.info("心跳包处理完成: {}", session.getId());
                return;
            }

            // 2. 处理业务消息
            if (WsMessageType.TYPING.getValue().equals(typeStr)) {
                handleTypingMessage(userId, rootNode);
            }
            // 3. 处理客户端发来的 ACK 确认消息
            else if (WsMessageType.ACK.getValue().equals(typeStr)) {
                handleAckMessage(userId, rootNode);
            }

        } catch (Exception e) {
            log.error("用户 [{}] 消息处理异常, payload: {}", userId, payload, e);
            sendMsg(userId, WsMessage.of(
                    WsMessageType.ERROR,
                    snowflakeIdGenerator.nextId(),
                    0L,
                    System.currentTimeMillis(),
                    "消息格式错误或处理失败"
            ));
        }
    }

    /**
     * 处理并响应心跳 Ping
     */
    private void handlePingMessage(WebSocketSession session, JsonNode rootNode) {
        try {
            long timestamp = rootNode.path("timestamp").asLong(System.currentTimeMillis());

            // 构造前端轻量级 Pong 响应：{ "type": "pong", "timestamp": <时间戳> }
            ObjectNode pongNode = objectMapper.createObjectNode();
            pongNode.put("type", WsMessageType.PONG.getValue());
            pongNode.put("timestamp", timestamp);

            // 直接发送底层消息，不走业务层的 sendMsg
            sendMessageToSessionSafely(session, pongNode.toString());
        } catch (Exception e) {
            log.error("响应心跳包失败", e);
        }
    }

    /**
     * 处理客户端的游标 ACK 包
     */
    private void handleAckMessage(Long userId, JsonNode rootNode) {
        try {
            JsonNode dataNode = rootNode.path("data");
            if (!dataNode.isMissingNode()) {
                String deviceId = dataNode.path("deviceId").asText();
                long syncTimeMillis = dataNode.path("syncTime").asLong();
                if (deviceId != null && !deviceId.isEmpty() && syncTimeMillis > 0) {
                    java.time.LocalDateTime syncTime = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(syncTimeMillis), java.time.ZoneId.systemDefault());
                    userDeviceSyncService.updateSyncCursorInRedis(userId, deviceId, syncTime);
                }
            }
        } catch (Exception e) {
            log.error("用户 [{}] 处理 ACK 异常", userId, e);
        }
    }

    /**
     * 【定时任务】扫描僵尸连接
     * 每 30 秒执行一次，如果发现某个 Session 超过 90 秒未交互，主动剔除并释放资源
     */
    @Scheduled(fixedRate = 30000)
    public void checkZombieSessions() {
        long now = System.currentTimeMillis();

        // 使用 entrySet().removeIf 进行安全的高效并发遍历剔除
        userSessions.entrySet().removeIf(entry -> {
            Long userId = entry.getKey();
            WebSocketSession session = entry.getValue();

            if (session == null || !session.isOpen()) {
                return true; // 连接已关，踢出 Map
            }

            Long lastActiveTime = (Long) session.getAttributes().getOrDefault(LAST_ACTIVE_TIME_KEY, now);
            if (now - lastActiveTime > SESSION_TIMEOUT_MS) {
                log.warn("用户 [{}] 心跳超时(90s未响应)，强制断开僵尸连接", userId);
                try {
                    // 主动关闭连接，状态码 1011 (服务器异常/内部状态错误) 或 1000 (正常) 均可
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException e) {
                    log.error("强制关闭用户 [{}] 连接异常", userId, e);
                }
                return true; // 从 Map 中移除
            }
            return false;
        });
    }

    /**
     * 更新 Session 活跃时间
     */
    private void updateSessionActiveTime(WebSocketSession session) {
        if (session.isOpen()) {
            session.getAttributes().put(LAST_ACTIVE_TIME_KEY, System.currentTimeMillis());
        }
    }

    /**
     * 提取出的、线程安全的底层消息发送方法
     */
    private void sendMessageToSessionSafely(WebSocketSession session, String jsonMessage) {
        if (session != null && session.isOpen()) {
            // WebSocket 必须对 Session 进行加锁，防止多线程同时发送导致流错乱 (IllegalStateException)
            synchronized (session) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    log.error("底层发送消息给用户 [{}] 失败: {}", getUserId(session), e.getMessage());
                }
            }
        }
    }

    /**
     * 对外暴露的业务消息发送接口
     */
    public void sendMsg(Long receiverId, WsMessage<?> message) {
        WebSocketSession session = userSessions.get(receiverId);

        if (session == null || !session.isOpen()) {
            // TODO：写入 Redis 离线消息队列，或通过 APNs/华为推送 进行离线唤醒
            log.debug("用户 [{}] 离线，消息已转入离线处理队列 (待实现)", receiverId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            sendMessageToSessionSafely(session, json);
        } catch (Exception e) {
            log.error("序列化并发送消息给用户 [{}] 失败", receiverId, e);
            // 严重异常时尝试从缓存清理并断开
            removeSession(session);
        }
    }

    private void handleTypingMessage(Long senderId, JsonNode rootNode) {
        JsonNode dataNode = rootNode.path("data");
        if (dataNode.isMissingNode()) return;

        long receiverId = dataNode.path("receiverId").asLong();

        WsMessage<Void> typingMsg = WsMessage.of(
                WsMessageType.TYPING,
                snowflakeIdGenerator.nextId(),
                senderId,
                System.currentTimeMillis(),
                null
        );

        sendMsg(receiverId, typingMsg);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        removeSession(session);
        log.error("用户 [{}] 连接发生异常断开", getUserId(session), exception);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus closeStatus) {
        removeSession(session);
        log.info("用户 [{}] 正常下线，状态码: {}", getUserId(session), closeStatus.getCode());
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
        if(session == null) {
            return;
        }
        Long userId = getUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
            // 这里可以触发一次强制刷盘，保证离线时游标立刻落库，避免 Redis 和 MySQL 产生窗口期差异
            try {
                userDeviceSyncService.flushRedisCursorsToDb();
            } catch (Exception e) {
                log.error("触发游标落库失败", e);
            }
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
        }
    }
}