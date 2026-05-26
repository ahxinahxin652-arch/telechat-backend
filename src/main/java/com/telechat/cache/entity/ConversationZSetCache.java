package com.telechat.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationZSetCache {
    /**
     * 会话ID (作为 ZSet 的 Member)
     */
    private Long conversationId;

    /**
     * 最终计算出的排序分值 (作为 ZSet 的 Score)
     */
    private Double score;

    /**
     * 【辅助字段】最后一条消息的时间戳
     * 用于在预热逻辑中重新计算 Score
     */
    private LocalDateTime lastMessageTime;

    /**
     * 【辅助字段】是否置顶
     * 1: 置顶, 0: 普通
     */
    private boolean isToped;
}
