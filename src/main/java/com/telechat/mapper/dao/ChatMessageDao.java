package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.telechat.mapper.ChatMessageMapper;
import com.telechat.pojo.entity.ChatMessage;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ChatMessageDao extends ServiceImpl<ChatMessageMapper, ChatMessage> {
    
    public List<ChatMessage> selectMessagesAfterTime(List<Long> conversationIds, LocalDateTime syncTime) {
        if (conversationIds == null || conversationIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectMessagesAfterTime(conversationIds, syncTime);
    }
}
