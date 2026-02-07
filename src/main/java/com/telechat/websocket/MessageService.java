package com.telechat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telechat.pojo.dto.ws.ContactApplyNotification;
import com.telechat.pojo.dto.ws.WsMessage;
import com.telechat.pojo.enums.WsMessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;

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

}