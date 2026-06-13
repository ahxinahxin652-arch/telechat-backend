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

    public List<ChatMessage> selectMessagesNewer(Long conversationId, Long anchorSeqId, Integer limit) {
        return baseMapper.selectMessagesNewer(conversationId, anchorSeqId, limit);
    }

    public List<ChatMessage> selectMessagesOlder(Long conversationId, Long anchorSeqId, Integer limit) {
        return baseMapper.selectMessagesOlder(conversationId, anchorSeqId, limit);
    }

    public ChatMessage selectMessageBySeqId(Long conversationId, Long seqId) {
        return baseMapper.selectMessageBySeqId(conversationId, seqId);
    }
}
