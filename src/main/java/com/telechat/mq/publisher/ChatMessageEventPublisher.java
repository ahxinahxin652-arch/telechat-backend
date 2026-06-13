package com.telechat.mq.publisher;

import com.telechat.constant.MessageQueueConstant;
import com.telechat.mq.event.ChatMessageEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatMessageEventPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void publishChatMessageEvent(ChatMessageEvent event) {
        log.info("Sending ChatMessageEvent, seqId: {}", event.getMessage().getSeqId());
        rabbitTemplate.convertAndSend(
                MessageQueueConstant.EventTopicExchange,
                MessageQueueConstant.RK_CHAT_MESSAGE,
                event
        );
    }
}
