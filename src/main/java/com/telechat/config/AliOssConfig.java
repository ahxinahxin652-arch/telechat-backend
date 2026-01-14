/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/6/19 下午4:52
 */
package com.telechat.config;


import com.telechat.properties.AliOssProperties;
import com.telechat.util.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * 配置类，用于配置阿里云OSS相关信息。服务启动时自动创建一个AliOssUtil对象，并注入到Spring容器中。
 * */
@Configuration
@Slf4j
public class AliOssConfig {

    @Bean // 把方法的返回值作为Bean对象存入Spring容器中
    /*
     * 注入阿里云OSS相关信息
     * */
    @ConditionalOnMissingBean // 如果容器中没有这个Bean，则创建至多一个
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云OSS工具类客户端对象：{}", aliOssProperties);
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName(),
                aliOssProperties.getMaxAvatarSize()
        );
    }

}
