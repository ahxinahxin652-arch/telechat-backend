package com.telechat.pojo.dto.ws;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请通知消息体
 */
@Data
@Builder
public class ContactApplyNotification {
    // 申请记录ID (方便前端点击同意/拒绝)
    private Long applyId;
    // 申请人ID
    private Long senderId;
    // 申请人昵称
    private String nickname;
    // 申请人头像
    private String avatar;
    // 申请附言 (我是XX...)
    private String description;
    // 时间
    private LocalDateTime createTime;
}