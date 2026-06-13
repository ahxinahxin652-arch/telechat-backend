package com.telechat.mq.consumer;

import com.telechat.constant.MessageQueueConstant;
import com.telechat.mq.event.ChatMessageEvent;
import com.telechat.websocket.TelechatWebSocketHandler;
import com.telechat.websocket.message.WsMessage;
import com.telechat.pojo.enums.WsMessageType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ChatMessageEventConsumer {

    @Resource
    private TelechatWebSocketHandler webSocketHandler;

    @RabbitListener(queues = MessageQueueConstant.Queue_Chat_Message)
    public void handleChatMessageEvent(ChatMessageEvent event) {
        log.info("Received ChatMessageEvent from MQ, seqId: {}", event.getMessage().getSeqId());
        try {
            WsMessage<com.telechat.pojo.entity.ChatMessage> wsMessage = WsMessage.of(
                    WsMessageType.CHAT,
                    event.getMessage().getId(),
                    event.getSenderId(),
                    event.getMessage().getCreatedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    event.getMessage()
            );

            // 推送给所有接收者
            List<Long> receiverIds = event.getReceiverIds();
            if (receiverIds != null && !receiverIds.isEmpty()) {
                for (Long receiverId : receiverIds) {
                    // 发送方如果是自己，且多端登录，也需要推给自己（本期暂不支持多端，先只推给在线的人）
                    // webSocketHandler内部如果用户离线会直接忽略并记录log
                    webSocketHandler.sendMsg(receiverId, wsMessage);
                    log.info("Successfully pushed ChatMessage via WS to user: {}", receiverId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process ChatMessageEvent in consumer", e);
            // 这里抛出异常以便 MQ 进行重试
            throw e;
        }
    }
}
