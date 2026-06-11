package com.telechat.service.impl;

import com.telechat.constant.ExceptionConstant;
import com.telechat.constant.RedisConstant;
import com.telechat.exception.exceptions.ConversationException;
import com.telechat.mapper.dao.ChatMessageDao;
import com.telechat.mapper.dao.ConversationDao;
import com.telechat.mapper.dao.ConversationMemberDao;
import com.telechat.pojo.entity.ChatMessage;
import com.telechat.pojo.entity.Conversation;
import com.telechat.service.ChatMessageService;
import com.telechat.util.SnowflakeIdGenerator;
import com.telechat.websocket.TelechatWebSocketHandler;
import com.telechat.pojo.enums.WsMessageType;
import com.telechat.websocket.message.WsMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    @Resource
    private ChatMessageDao chatMessageDao;

    @Resource
    private ConversationDao conversationDao;

    @Resource
    private ConversationMemberDao conversationMemberDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Resource
    private TelechatWebSocketHandler telechatWebSocketHandler;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ChatMessage sendMessage(Long senderId, Long conversationId, String content, Integer messageType) {
        // 1. 简单校验：非系统消息需判断发送者是否在群/好友会话中
        if (messageType < 100) {
            // 系统消息类型>=100，不做强制成员判断（有时系统由官方号发送）
            if (conversationMemberDao.selectByConversationIdAndUserId(conversationId, senderId) == null) {
                throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, "您不在该会话中，无法发送消息");
            }
        }

        // 2. 原子生成递增 SeqId (使用 Redis INCR)
        String seqKey = RedisConstant.CONVERSATION_SEQ_ID + conversationId;
        Long seqId = stringRedisTemplate.opsForValue().increment(seqKey);
        if (seqId == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, "序列号生成失败");
        }

        // 3. 构建并插入消息
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = ChatMessage.builder()
                .id(snowflakeIdGenerator.nextId())
                .conversationId(conversationId)
                .seqId(seqId)
                .senderId(senderId)
                .content(content)
                .messageType(messageType)
                .createdTime(now)
                .status((byte) 1) // 已发送
                .build();
        
        chatMessageDao.save(message);

        // 4. 更新会话的 last_message_content 和 last_message_time
        // 截取预览文本
        String previewText = content;
        if (content != null && content.length() > 50) {
            previewText = content.substring(0, 50) + "...";
        }
        
        // 增量架构核心：更新 c.last_message_time 以驱动客户端拉取
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageContent(previewText);
        conversation.setLastMessageTime(now);
        conversationDao.update(conversation);

        // 同步更新发送者的已读消息游标，防止自己发的消息变成未读
        conversationMemberDao.updateLastReadMessageId(conversationId, senderId, message.getSeqId());

        // TODO: 可选，广播 WebSocket / RabbitMQ
        // 此处进行简单的 WebSocket 实时推送
        try {
            java.util.List<com.telechat.pojo.entity.ConversationMember> members = conversationMemberDao.selectMembersByConversationId(conversationId);
            if (members != null) {
                WsMessage<ChatMessage> wsMessage = WsMessage.of(
                        WsMessageType.CHAT,
                        message.getId(),
                        senderId,
                        now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        message
                );
                for (com.telechat.pojo.entity.ConversationMember member : members) {
                    if (!member.getUserId().equals(senderId)) {
                        telechatWebSocketHandler.sendMsg(member.getUserId(), wsMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("WebSocket推送聊天消息失败", e);
        }

        return message;
    }

    @Override
    public void markMessageAsRead(Long userId, Long conversationId, Long seqId) {
        if (conversationId == null || seqId == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, "参数错误");
        }

        com.telechat.pojo.entity.ConversationMember member = conversationMemberDao.selectByConversationIdAndUserId(conversationId, userId);
        if (member == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, "您不在该会话中");
        }

        Long currentLastRead = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();

        // 只有新的游标大于老游标，才进行覆盖更新，防止游标倒退
        if (seqId > currentLastRead) {
            conversationMemberDao.updateLastReadMessageId(conversationId, userId, seqId);
        }
    }
}
