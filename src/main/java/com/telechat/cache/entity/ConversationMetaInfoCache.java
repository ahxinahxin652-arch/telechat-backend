package com.telechat.cache.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话动态信息缓存（Hash）
 * 缓存会话的动态信息：标题，头像等，不缓存动态信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMetaInfoCache {
    // 会话id
    private Long id;

    // 最新消息id
    private Long lastMessageId;

    // 最新消息内容(只截取前15个字)
    @Max(30)
    private String lastMessageContent;

    // 最新消息时间
    private LocalDateTime lastMessageTime;

    // 防止缓存穿透的标记方法
    @JsonIgnore
    public boolean isNullPlaceholder() {
        return this.id != null && this.id == -1L;
    }

}
