package com.telechat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("user_device_sync")
public class UserDeviceSync {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    
    private Long userId;
    
    private String deviceId;
    
    private Byte clientType; // 1-Web, 2-Desktop, 3-Mobile
    
    private LocalDateTime lastSyncTime;
    
    private Byte status; // 1-Online/Active, 0-Offline/Kicked
    
    private LocalDateTime createdTime;
    
    private LocalDateTime updatedTime;
}
