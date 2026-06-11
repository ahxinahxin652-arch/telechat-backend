package com.telechat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.telechat.mapper.UserDeviceSyncMapper;
import com.telechat.pojo.entity.UserDeviceSync;
import com.telechat.service.UserDeviceSyncService;
import com.telechat.util.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Service
public class UserDeviceSyncServiceImpl extends ServiceImpl<UserDeviceSyncMapper, UserDeviceSync> implements UserDeviceSyncService {

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Redis Key for storing sync cursors: Hash -> Key: "device:sync:cursor", HashKey: "{userId}:{deviceId}", Value: "timestamp_in_millis"
    private static final String REDIS_SYNC_CURSOR_KEY = "device:sync:cursor";

    @Override
    public UserDeviceSync getOrInitDeviceSync(Long userId, String deviceId, Byte clientType, boolean isFirstLoginEver) {
        LambdaQueryWrapper<UserDeviceSync> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDeviceSync::getUserId, userId)
                .eq(UserDeviceSync::getDeviceId, deviceId);
        
        UserDeviceSync deviceSync = this.getOne(queryWrapper);
        
        if (deviceSync == null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime initialSyncTime;
            
            if (isFirstLoginEver) {
                // 绝对首次登录，没有任何历史消息，时间置为当前
                initialSyncTime = now;
            } else {
                // 老用户新设备，漫游近30天消息
                initialSyncTime = now.minusDays(30);
            }

            deviceSync = UserDeviceSync.builder()
                    .id(snowflakeIdGenerator.nextId())
                    .userId(userId)
                    .deviceId(deviceId)
                    .clientType(clientType)
                    .lastSyncTime(initialSyncTime)
                    .status((byte) 1)
                    .createdTime(now)
                    .build();
            
            this.save(deviceSync);
        } else {
            // 如果设备已经存在，更新状态为活跃
            deviceSync.setStatus((byte) 1);
            this.updateById(deviceSync);
        }
        
        return deviceSync;
    }

    @Override
    public void updateSyncTime(Long userId, String deviceId, LocalDateTime syncTime) {
        LambdaQueryWrapper<UserDeviceSync> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(UserDeviceSync::getUserId, userId)
                .eq(UserDeviceSync::getDeviceId, deviceId);
        
        UserDeviceSync updateObj = new UserDeviceSync();
        updateObj.setLastSyncTime(syncTime);
        this.update(updateObj, updateWrapper);
    }

    @Override
    public void updateSyncCursorInRedis(Long userId, String deviceId, LocalDateTime syncTime) {
        String hashKey = userId + ":" + deviceId;
        String timestampStr = String.valueOf(syncTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        stringRedisTemplate.opsForHash().put(REDIS_SYNC_CURSOR_KEY, hashKey, timestampStr);
    }

    /**
     * 定时任务：每隔一分钟将 Redis 中的游标批量刷入数据库
     */
    @Scheduled(fixedRate = 60000)
    @Override
    public void flushRedisCursorsToDb() {
        try {
            // 使用 HSCAN 遍历 Redis 哈希表
            Cursor<Map.Entry<Object, Object>> cursor = stringRedisTemplate.opsForHash().scan(
                    REDIS_SYNC_CURSOR_KEY,
                    ScanOptions.scanOptions().count(100).build()
            );

            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                String hashKey = (String) entry.getKey(); // "{userId}:{deviceId}"
                String timestampStr = (String) entry.getValue();

                String[] parts = hashKey.split(":");
                if (parts.length != 2) continue;

                Long userId = Long.valueOf(parts[0]);
                String deviceId = parts[1];
                long timestamp = Long.parseLong(timestampStr);

                LocalDateTime syncTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

                // 更新数据库
                updateSyncTime(userId, deviceId, syncTime);
                
                // 从 Redis 中删除已处理的游标
                stringRedisTemplate.opsForHash().delete(REDIS_SYNC_CURSOR_KEY, hashKey);
            }
        } catch (Exception e) {
            log.error("Flush Redis cursors to DB failed", e);
        }
    }
}
