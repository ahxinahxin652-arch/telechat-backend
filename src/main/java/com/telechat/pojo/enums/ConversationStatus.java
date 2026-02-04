package com.telechat.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 会话状态枚举
 * 管理会话的生命周期：正常、解散、封禁等
 */
@Getter
@AllArgsConstructor // 核心：自动生成带参构造函数，解决你之前的报错问题
public enum ConversationStatus {

    /**
     * 0: 已解散/失效 (群主主动解散，或私聊已断开)
     */
    DISBANDED(0, "已解散"),

    /**
     * 1: 正常 (活跃状态)
     */
    NORMAL(1, "正常"),

    /**
     * 2: 系统封禁 (因违规被平台强制关闭)
     */
    BANNED(2, "系统封禁");

    @EnumValue   // 告诉 Mybatis-Plus 存入数据库的是这个 code (0, 1, 2)
    @JsonValue   // 告诉 Jackson 序列化给前端的是这个 code
    private final int code;

    private final String desc;

    // 可选：静态工具方法，方便手动转换
    public static ConversationStatus of(Integer code) {
        if (code == null) return null;
        for (ConversationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
