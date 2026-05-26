package com.telechat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模板对象...");
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 1. 构建自定义的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 【关键点1】注册 JavaTimeModule，解决 LocalDateTime 序列化异常
        objectMapper.registerModule(new JavaTimeModule());

        // 【可选】配置不将日期写为时间戳，而是写为 ISO-8601 字符串 (2026-02-05T...)，方便在 Redis 工具中查看
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 【关键点2】激活默认类型信息
        // 这一步是为了让 Redis 知道存进去的是 ContactApplyCache 类，而不是一个普通的 LinkedHashMap
        // 如果没有这一步，反序列化时会报错 ClassCastException
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 2. 使用自定义的 ObjectMapper 创建序列化器
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 3. 设置序列化器
        // Key 采用 String 序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer()); // HashKey 通常也是 String

        // Value 采用自定义的 JSON 序列化
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        // 初始化RedisTemplate
        redisTemplate.afterPropertiesSet();

        log.info("redis模板对象创建完成");
        return redisTemplate;
    }
}