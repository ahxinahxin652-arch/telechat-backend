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
}
