package com.telechat.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telechat.constant.MessageQueueConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitConfig {

    // 1. 先定义 JSON 序列化转换器 (解决 Java8 时间类型问题)
    @Bean
    public MessageConverter messageConverter() {
        log.info("开始创建Rabbitmq模板");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connectionFactory) {
        // 获取bean
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        // 应用 application.yml 中的默认配置（如重试、超时）
        configurer.configure(rabbitTemplate, connectionFactory);

        // 1. 必须设置为 true，否则消息路由失败不会触发 ReturnCallback
        rabbitTemplate.setMandatory(true);

        // 2. 设置 ConfirmCallback (确认消息是否到达交换机)
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            // 获取 Publisher 传入的唯一 messageID
            String messageId = correlationData != null ? correlationData.getId() : "未知ID";
            if (ack) {
                log.debug("消息成功发送到交换机: id={}", messageId);
            } else {
                log.error("消息发送到交换机失败: 原因={}, id={}", cause, correlationData != null ? correlationData.getId() : "null");
                // 此处可添加基于 Redis 的重试逻辑
            }
        });

        // 3. 设置 ReturnCallback (确认消息是否路由到队列)
        // 只有【路由失败】(例如写错了 RoutingKey) 才会触发这个回调！成功进入队列是不会触发的！
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由队列失败！交换机: {}, 路由键: {}, 回应码: {}, 原因: {}, 消息: {}",
                    returned.getExchange(), returned.getRoutingKey(),
                    returned.getReplyCode(), returned.getReplyText(), returned.getMessage());
        });
        return rabbitTemplate;
    }

    // 3. 配置 死信恢复器
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate){
        log.info("创建Rabbitmq模板完成");
        // 设置 error 消息的 RoutingKey (带有异常堆栈信息的 header 会被自动追加)
        return new RepublishMessageRecoverer(rabbitTemplate,
                MessageQueueConstant.ErrorDirectExchange,
                MessageQueueConstant.RK_ERROR);
    }
    // ================== 死信/异常队列配置 ==================
    @Bean
    public DirectExchange errorExchange() {
        return new DirectExchange(MessageQueueConstant.ErrorDirectExchange);
    }

    @Bean
    public Queue errorQueue() {
        return new Queue(MessageQueueConstant.Queue_Error, true);
    }

    @Bean
    public Binding errorBinding() {
        return BindingBuilder.bind(errorQueue()).to(errorExchange()).with(MessageQueueConstant.RK_ERROR);
    }

    // ================== 业务队列配置 ==================
    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(MessageQueueConstant.EventTopicExchange);
    }

    // 聊天消息: 队列 / 绑定
    @Bean
    public Queue chatMessageQueue(){
        return new Queue(MessageQueueConstant.Queue_Chat_Message, true);
    }

    @Bean
    public Binding bingChatMessage(){
        return BindingBuilder.bind(chatMessageQueue()).to(eventExchange()).with(MessageQueueConstant.RK_CHAT_MESSAGE);
    }

    // 好友与会话队列: 队列 / 绑定
    @Bean
    public Queue contactConversationQueue(){
        return new Queue(MessageQueueConstant.Queue_Contact_Conversation, true);
    }

    @Bean
    public Binding bingContactConversation(){
        return BindingBuilder.bind(contactConversationQueue()).to(eventExchange()).with(MessageQueueConstant.RK_CONTACT_CONVERSATION);
    }
}
