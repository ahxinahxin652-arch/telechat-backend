package com.telechat.pojo.enums;

import lombok.Getter;

/**
 * WebSocket 消息类型枚举
 * 统一管理所有消息类型，避免魔法字符串
 */
@Getter
public enum WsMessageType {
    // 基础类型
    SYSTEM("system"),           // 系统通知
    ERROR("error"),             // 错误提示
    HEARTBEAT("heartbeat"),     // 心跳包
    
    // 业务类型
    CHAT("chat"),               // 聊天消息
    TYPING("typing"),           // 正在输入...
    CONTACT_APPLY("contact_apply"), // 好友申请
    CONTACT_REPLY("contact_reply"); // 好友申请处理结果

    private final String value;

    WsMessageType(String value) {
        this.value = value;
    }
}