package com.telechat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.telechat.pojo.entity.UserDeviceSync;

import java.time.LocalDateTime;

public interface UserDeviceSyncService extends IService<UserDeviceSync> {
    
    /**
     * 获取或初始化设备同步状态
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param clientType 客户端类型
     * @param isFirstLoginEver 是否是该用户的历史首次登录
     * @return UserDeviceSync
     */
    UserDeviceSync getOrInitDeviceSync(Long userId, String deviceId, Byte clientType, boolean isFirstLoginEver);
    
    /**
     * 更新设备的最新同步时间（主要用于 HTTP 接口直接拉取）
     */
    void updateSyncTime(Long userId, String deviceId, LocalDateTime syncTime);

    /**
     * 更新 Redis 中的游标缓冲
     */
    void updateSyncCursorInRedis(Long userId, String deviceId, LocalDateTime syncTime);
    
    /**
     * 定时任务：将 Redis 中的同步游标持久化到数据库
     */
    void flushRedisCursorsToDb();
}
