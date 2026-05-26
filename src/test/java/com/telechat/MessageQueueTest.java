package com.telechat;

import com.telechat.constant.MessageQueueConstant;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest
public class MessageQueueTest {
    @Resource
    private RabbitTemplate rabbitTemplate;
}
