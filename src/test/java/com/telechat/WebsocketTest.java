package com.telechat;

import com.telechat.websocket.TelechatWebSocketHandler;
import com.telechat.websocket.message.WsMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.client.WebSocketClient;

@Slf4j
@SpringBootTest
public class WebsocketTest {
    @Resource
    private TelechatWebSocketHandler webSocketHandler;

    @Test
    public void test() {
        log.info("开始发消息");
        WsMessage<String> wsMessage = new WsMessage<>();
        webSocketHandler.sendMsg(287967118450888704L, wsMessage);
        log.info("结束发消息");
    }
}
