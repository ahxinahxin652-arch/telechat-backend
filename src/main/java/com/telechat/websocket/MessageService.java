package com.telechat.websocket;

import com.telechat.websocket.message.ContactApplyNotification;
import com.telechat.websocket.message.ContactCreateWsNotification;
import com.telechat.websocket.message.WsMessage;
import com.telechat.pojo.enums.WsMessageType;
import com.telechat.websocket.message.GroupCreateWsNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageService {
    
    @Autowired
    private TelechatWebSocketHandler webSocketHandler;

    /**
     * 发送好友申请通知
     * @param receiverId 接收者ID (也就是被加的那个人)
     * @param notification 通知内容
     */
    public void sendContactApplyNotification(Long receiverId, ContactApplyNotification notification) {
        try {
            // 构建标准消息
            WsMessage<Object> message = WsMessage.of(
                    WsMessageType.CONTACT_APPLY,
                    notification.getApplyId(),
                    notification.getSenderId(),
                    notification.getTimestamp(),
                    notification
            );

            // 发送
            webSocketHandler.sendMsg(receiverId, message);
            log.info("好友申请通知已推送给用户: {}", receiverId);
        } catch (Exception e) {
            log.error("好友申请通知推送失败", e);
            // 推送失败不应该影响业务主流程，所以这里吞掉异常或者记录日志即可
        }
    }

    /**
     * 发送群聊创建通知
     * @param receiverId 接收者ID (也就是被加的那个人)
     * @param notification 通知内容
     */
    public void sendContactCreateNotification(Long receiverId, ContactCreateWsNotification notification) {
        try {
            // 构建标准消息
            WsMessage<Object> message = WsMessage.of(
                    WsMessageType.CONTACT_CREATE,
                    notification.getConversationVO().getId(),
                    notification.getUserId(),
                    notification.getTimestamp(),
                    notification
            );

            // 发送
            webSocketHandler.sendMsg(receiverId, message);
            log.info("私聊创建通知已推送给用户: {}", receiverId);
        } catch (Exception e) {
            log.error("私聊创建通知推送失败", e);
        }
    }

    /**
     * 发送群聊创建通知
     * @param receiverId 接收者ID (也就是被加的那个人)
     * @param notification 通知内容
     */
    public void sendGroupCreateNotification(Long receiverId, GroupCreateWsNotification notification) {
        try {
            // 构建标准消息
            WsMessage<Object> message = WsMessage.of(
                    WsMessageType.GROUP_CREATE,
                    notification.getConversationVO().getId(),
                    notification.getOwnerId(),
                    notification.getTimestamp(),
                    notification
            );

            // 发送
            webSocketHandler.sendMsg(receiverId, message);
            log.info("群聊创建通知已推送给用户: {}", receiverId);
        } catch (Exception e) {
            log.error("群聊创建通知推送失败", e);
        }
    }
}