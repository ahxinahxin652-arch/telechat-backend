package com.telechat.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConversationType {
    PRIVATE(0,"PRIVATE"),
    GROUP(1,"GROUP"),
    CHANNEL(2,"CHANNEL");

    @EnumValue
    @JsonValue
    private final int code;

    private final String desc;

    public static String getDesc(int code) {
        for (ConversationType value : ConversationType.values()) {
            if (value.code == code) {
                return value.desc;
            }
        }
        return null;
    }

}
