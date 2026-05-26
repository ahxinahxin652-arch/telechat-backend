package com.telechat.cache.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telechat.pojo.enums.ConversationStatus;
import com.telechat.pojo.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话静态信息缓存（String）
 * 缓存会话的静态信息：标题，头像等，不缓存动态信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationStaticInfoCache {
    // 会话id
    private Long id;

    // 会话类型：0私聊 1群聊 2频道
    private ConversationType type;

    // 群聊/频道标题（私聊从UserInfoCache中获取）
    private String title;

    // 群聊/频道头像（私聊从UserInfoCache中获取）
    private String avatar;

    // 群主ID
    private Long ownerId;

    // UniqueKey
    private String uniqueKey;

    // 状态
    private ConversationStatus status;

    // 防止缓存穿透的标记方法
    @JsonIgnore
    public boolean isNullPlaceholder() {
        return this.id != null && this.id == -1L;
    }
}
