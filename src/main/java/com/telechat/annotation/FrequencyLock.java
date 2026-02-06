package com.telechat.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;
/**
* 防止重复操作的锁
* */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FrequencyLock {

    /**
     * 锁的 Key，支持 SpEL 表达式
     * 例如："'apply:' + #userId" 或 "'handle:' + #dto.id"
     */
    String key();

    /**
     * 锁等待时间，默认 0 (立即失败，Fail-Fast模式)
     * 如果设置为 > 0，则会阻塞等待直到获取锁或超时
     */
    long waitTime() default 0;

    /**
     * 锁自动释放时间，默认 10 秒
     * 如果使用 Redisson 的看门狗机制，可以忽略此项（设为 -1）
     */
    long leaseTime() default 10;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 获取锁失败的错误提示信息
     */
    String msg() default "操作过于频繁，请稍后再试";
}
