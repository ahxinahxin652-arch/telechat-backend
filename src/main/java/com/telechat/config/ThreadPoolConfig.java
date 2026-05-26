package com.telechat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "preHeatExecutor")
    public Executor preHeatExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1. 核心线程数：根据 CPU 核心数定，比如 8
        executor.setCorePoolSize(8);
        // 2. 最大线程数：严格限制，比如 16
        executor.setMaxPoolSize(16);
        // 3. 阻塞队列：如果 16 个线程都在忙，剩下的任务在队列里排队
        executor.setQueueCapacity(500); 
        // 4. 拒绝策略：如果队列也满了，直接丢弃新任务 (AbortPolicy 或 DiscardPolicy)
        // 因为预热是增益操作，丢了就丢了，下次滑动还会触发，绝不能拖垮主业务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setThreadNamePrefix("conv-preheat-");
        executor.initialize();
        return executor;
    }
}