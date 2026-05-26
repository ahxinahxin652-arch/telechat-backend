package com.telechat.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContactApplyStatus {
    /**
     * 0: 待处理 (Pending) - 默认状态
     */
    PENDING(0, "PENDING"),

    /**
     * 1: 已同意 (Accepted)
     */
    ACCEPTED(1, "ACCEPTED"),

    /**
     * 2: 已拒绝 (Rejected)
     */
    REJECTED(2, "REJECTED");

    @EnumValue   // 存入数据库的值 (0, 1, 2)
    @JsonValue   // 前端看到的 JSON 值
    private final int code;

    private final String desc;

    // 静态工具方法
    public static ContactApplyStatus of(Integer code) {
        if (code == null) return null;
        for (ContactApplyStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
