package com.telechat.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConversationMemberRole {
    OWNER(0,"OWNER"),
    ADMIN(1,"ADMIN"),
    MEMBER(2,"MEMBER");

    @EnumValue
    @JsonValue
    private final int code;

    private final String desc;

    public static String getDesc(int code) {
        for (ConversationMemberRole value : ConversationMemberRole.values()) {
            if (value.code == code) {
                return value.desc;
            }
        }
        return null;
    }
}
