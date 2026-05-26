package com.telechat.pojo.enums.mq;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContactConversationType {
    /**
     * 1: 好友申请 (CONTACT_APPLY) - 默认状态
     */
    CONTACT_APPLY(1, "contact_apply"),

    /**
     * 2: 同意申请 (APPLY_AGREE)
     */
    APPLY_AGREE(2, "apply_agree"),

    /**
     * 3: 拒绝申请 (APPLY_REFUSE)
     */
    APPLY_REFUSE(3, "apply_refuse"),

    /**
     * 4: 删除好友 (CONTACT_DELETE)
     */
    CONTACT_DELETE(4, "contact_delete"),

    /**
     * 5: 创建群聊 (GROUP_CREATE)
     */
    GROUP_CREATE(5, "group_create"),

    /**
     * 6: 踢出群聊 (GROUP_CREATE)
     */
    GROUP_REMOVE(6, "group_remove"),

    /**
     * 7: 解散群聊 (GROUP_DISBAND)
     */
    GROUP_DISBAND(7, "group_disband");

    @EnumValue   // 存入数据库的值 (0, 1, 2)
    @JsonValue   // 前端看到的 JSON 值
    private final int code;

    private final String desc;

    // 静态工具方法
    public static ContactConversationType of(Integer code) {
        if (code == null) return null;
        for (ContactConversationType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
