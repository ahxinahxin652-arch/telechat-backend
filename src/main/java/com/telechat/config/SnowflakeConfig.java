package com.telechat.config;

import com.telechat.util.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfig {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        // 这里可以硬编码，也可以从配置文件读取
        // 参数1: datacenterId (0-31), 参数2: workerId (0-31)
        //TODO 在分布式部署时，不同机器的 workerId 应该不同，可以从配置文件读取
        return new SnowflakeIdGenerator(1, 1);
    }
}