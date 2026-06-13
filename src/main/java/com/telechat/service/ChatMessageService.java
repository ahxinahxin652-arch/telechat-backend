package com.telechat.service;

import com.telechat.pojo.entity.ChatMessage;

public interface ChatMessageService {
    
    /**
     * 发送消息
     * @param senderId 发送者ID
     * @param conversationId 会话ID
     * @param content 消息内容
     * @param messageType 消息类型 (0:文本, 1:图片, 100:加好友系统消息, 等)
     * @return ChatMessage
     */
    ChatMessage sendMessage(Long senderId, Long conversationId, String content, Integer messageType);
    /**
     * 更新会话的已读游标
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @param seqId          已读的最大seqId
     */
    void markMessageAsRead(Long userId, Long conversationId, Long seqId);

    /**
     * 拉取历史消息(锚点分页)
     * @param userId         当前用户
     * @param conversationId 会话ID
     * @param anchorSeqId    锚点消息序列号
     * @param limit          限制条数
     * @param direction      方向 older/newer/around
     * @return 历史消息列表
     */
    java.util.List<ChatMessage> getHistoryMessages(Long userId, Long conversationId, Long anchorSeqId, Integer limit, String direction);
}
