package com.telechat.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTemplateUtil {
    private final RedisTemplate<String, Object> redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 预加载 Lua 脚本，提升性能，避免每次执行都重新编译脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        // Lua 脚本逻辑：
        // 1. 获取 Key 的值 (redis.call('get', KEYS[1]))
        // 2. 判断是否等于传入的 requestId (ARGV[1])
        // 3. 如果相等则删除 (redis.call('del', KEYS[1])) 并返回 1
        // 4. 不相等则返回 0
        UNLOCK_SCRIPT.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    //------------------------------------------------分布式锁相关方法------------------------------------------------------------------

    /**
     * 尝试获取锁 (非阻塞，立即返回结果)
     *
     * @param lockKey    锁的 Key
     * @param requestId  请求标识（通常是 UUID），解锁时用于身份验证
     * @param expireTime 过期时间
     * @param timeUnit   时间单位
     * @return boolean   是否获取成功
     */
    public boolean tryLock(String lockKey, String requestId, long expireTime, TimeUnit timeUnit) {
        // 对应 Redis 命令: SET key value NX PX milliseconds
        // setIfAbsent 利用了 Redis 的原子性，同时设置值和过期时间
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(
                lockKey,
                requestId,
                expireTime,
                timeUnit
        );
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放锁 (使用 Lua 脚本保证原子性)
     *
     * @param lockKey   锁的 Key
     * @param requestId 加锁时传入的唯一标识
     * @return boolean  是否释放成功
     */
    public boolean unlock(String lockKey, String requestId) {
        // 执行 Lua 脚本
        Long result = stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(lockKey), // KEYS[1]
                requestId                           // ARGV[1]
        );
        return result != null && result == 1L;
    }

    /**
     * 设置过期时间，并自动加上随机偏移量防止雪崩
     */
    public void expireWithRandom(String key, long timeout, TimeUnit unit) {
        // 加上 10% 以内的随机时间戳
        long randomMillis = ThreadLocalRandom.current().nextLong(unit.toMillis(timeout) / 10);
        long totalMillis = unit.toMillis(timeout) + randomMillis;

        redisTemplate.expire(key, totalMillis, TimeUnit.MILLISECONDS);
    }
}
