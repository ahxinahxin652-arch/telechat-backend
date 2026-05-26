package com.telechat.cache.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/**
 * 会话成员缓存
 * 缓存用户对于某个会话的状态：角色，是否免打扰，是否置顶等
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemberCache {
    private Long id;

    private Long conversationId;

    private Long userId;

    // 是否免打扰
    private boolean isMuted;

    // 是否置顶
    private boolean isToped;

    // 用户已读的最后一条消息ID（用于计算未读数）
    private Long lastReadMessageId;

    // 用户对于该会话的未读消息数
    @Max(100)
    private Integer unreadCount;

    private LocalDateTime joinedTime;

    // 防止缓存穿透的标记方法
    @JsonIgnore
    public boolean isNullPlaceholder() {
        return this.id != null && this.id == -1L;
    }
}
