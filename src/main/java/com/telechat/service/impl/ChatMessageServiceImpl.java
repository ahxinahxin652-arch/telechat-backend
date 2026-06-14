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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private com.telechat.mq.publisher.ChatMessageEventPublisher chatMessageEventPublisher;

    @Resource
    private com.telechat.mq.publisher.ContactConversationEventPublisher contactConversationEventPublisher;

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
        Boolean hasKey = stringRedisTemplate.hasKey(seqKey);
        if (hasKey == null || !hasKey) {
            Long initSeqId = 0L;
            Conversation conversation = conversationDao.selectById(conversationId);
            if (conversation != null && conversation.getLastMessageId() != null) {
                ChatMessage lastMessage = chatMessageDao.getById(conversation.getLastMessageId());
                if (lastMessage != null && lastMessage.getSeqId() != null) {
                    initSeqId = lastMessage.getSeqId();
                }
            }
            if (initSeqId == 0L) {
                LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getSeqId)
                        .last("limit 1");
                ChatMessage lastMsgByQuery = chatMessageDao.getOne(wrapper);
                if (lastMsgByQuery != null && lastMsgByQuery.getSeqId() != null) {
                    initSeqId = lastMsgByQuery.getSeqId();
                }
            }
            stringRedisTemplate.opsForValue().setIfAbsent(seqKey, String.valueOf(initSeqId));
        }

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

        // 5. 将消息发往 RabbitMQ，由特定消费者处理并发给 WebSocket (保证可靠投递和重试机制)
        try {
            java.util.List<com.telechat.pojo.entity.ConversationMember> members = conversationMemberDao.selectMembersByConversationId(conversationId);
            if (members != null) {
                java.util.List<Long> receiverIds = new java.util.ArrayList<>();
                for (com.telechat.pojo.entity.ConversationMember member : members) {
                    if (!member.getUserId().equals(senderId)) {
                        receiverIds.add(member.getUserId());
                    }
                }
                
                com.telechat.mq.event.ChatMessageEvent event = com.telechat.mq.event.ChatMessageEvent.builder()
                        .senderId(senderId)
                        .conversationId(conversationId)
                        .message(message)
                        .receiverIds(receiverIds)
                        .build();
                        
                chatMessageEventPublisher.publishChatMessageEvent(event);
            }
        } catch (Exception e) {
            log.error("将消息发往 RabbitMQ 失败", e);
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

            // 异步广播已读回执
            try {
                java.util.List<com.telechat.pojo.entity.ConversationMember> members = conversationMemberDao.selectMembersByConversationId(conversationId);
                if (members != null && !members.isEmpty()) {
                    java.util.List<Long> receiverIds = new java.util.ArrayList<>();
                    for (com.telechat.pojo.entity.ConversationMember m : members) {
                        receiverIds.add(m.getUserId());
                    }
                    if (!receiverIds.isEmpty()) {
                        com.telechat.mq.event.ContactConversationEvent event = com.telechat.mq.event.ContactConversationEvent.builder()
                                .contactConversationType(com.telechat.pojo.enums.mq.ContactConversationType.READ_RECEIPT)
                                .senderId(userId)
                                .allReceiverIds(receiverIds)
                                .conversationVO(com.telechat.pojo.vo.ConversationVO.builder().id(conversationId).build())
                                .description(String.valueOf(seqId))
                                .timestamp(System.currentTimeMillis())
                                .build();
                        contactConversationEventPublisher.publishContactConversationEvent(event);
                    }
                }
            } catch (Exception e) {
                log.error("发布已读回执MQ事件失败", e);
            }
        }
    }

    @Override
    public java.util.List<ChatMessage> getHistoryMessages(Long userId, Long conversationId, Long anchorSeqId, Integer limit, String direction) {
        if (conversationId == null || anchorSeqId == null || limit == null || limit <= 0) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, "参数错误");
        }

        if (limit > 100) {
            limit = 100;
        }

        // 校验权限
        com.telechat.pojo.entity.ConversationMember member = conversationMemberDao.selectByConversationIdAndUserId(conversationId, userId);
        if (member == null) {
            throw new ConversationException(ExceptionConstant.Judge_Query_Exception_Code, "您不在该会话中");
        }

        if (direction == null) {
            direction = "older";
        }

        java.util.List<ChatMessage> result;
        if ("newer".equalsIgnoreCase(direction)) {
            // 查询 anchorSeqId 之后的新消息，升序排列
            result = chatMessageDao.selectMessagesNewer(conversationId, anchorSeqId, limit);
        } else if ("around".equalsIgnoreCase(direction)) {
            // 查询前后的消息，前端需要看到 anchor 本身
            // 分成两步查：查前面的 limit/2，和后面的 limit/2
            int half = limit / 2;
            java.util.List<ChatMessage> olderList = chatMessageDao.selectMessagesOlder(conversationId, anchorSeqId, half);
            // olderList 查出来是降序，需要反转
            java.util.Collections.reverse(olderList);

            java.util.List<ChatMessage> newerList = chatMessageDao.selectMessagesNewer(conversationId, anchorSeqId, half);

            // anchorMsg 本身
            ChatMessage anchorMsg = chatMessageDao.selectMessageBySeqId(conversationId, anchorSeqId);

            result = new java.util.ArrayList<>();
            if (olderList != null) result.addAll(olderList);
            if (anchorMsg != null) result.add(anchorMsg);
            if (newerList != null) result.addAll(newerList);
        } else {
            // 默认 older: 查询 anchorSeqId 之前的旧消息，返回的结果是降序的，但在服务层我们一般要给前端按时间升序（从旧到新排列显示）
            // 查的时候按 seqId desc 查，拿到最新的一批历史，然后 reverse 给前端
            result = chatMessageDao.selectMessagesOlder(conversationId, anchorSeqId, limit);
            if (result != null && !result.isEmpty()) {
                java.util.Collections.reverse(result);
            }
        }

        return result;
    }
}
