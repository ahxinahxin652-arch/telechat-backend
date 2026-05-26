package com.telechat.mq.publisher;

import com.telechat.constant.MessageQueueConstant;
import com.telechat.mq.event.ContactConversationEvent;
import com.telechat.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContactConversationEventPublisher {
    @Resource
    private RabbitTemplate rabbitTemplate;

    // util
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    public void publishContactConversationEvent(ContactConversationEvent event){
        // 生产者消息回执机制
        String messageId = String.valueOf(snowflakeIdGenerator.nextId());
        CorrelationData correlationData = new CorrelationData(messageId);

        // 发送到对应队列
        rabbitTemplate.convertAndSend(
                MessageQueueConstant.EventTopicExchange,
                MessageQueueConstant.RK_CONTACT_CONVERSATION,
                event,
                correlationData
        );
    }
}
