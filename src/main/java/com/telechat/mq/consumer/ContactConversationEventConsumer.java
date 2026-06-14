package com.telechat.mq.consumer;

import com.telechat.cache.ContactApplyCacheService;
import com.telechat.constant.MessageQueueConstant;
import com.telechat.constant.RedisConstant;
import com.telechat.mq.event.ContactConversationEvent;
import com.telechat.websocket.MessageService;
import com.telechat.websocket.message.ContactApplyNotification;
import com.telechat.websocket.message.ContactCreateWsNotification;
import com.telechat.websocket.message.GroupCreateWsNotification;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ContactConversationEventConsumer {
    // redis
    @Resource
    private StringRedisTemplate stringRedisTemplate; // 推荐使用 StringRedisTemplate 执行 Lua

    @Resource
    private ContactApplyCacheService contactApplyCacheService;

    // ws
    @Resource
    private MessageService messageService;

    // 预加载脚本，提升性能
    private static final DefaultRedisScript<Long> BATCH_ZADD_IF_EXISTS_SCRIPT;

    static {
        String script =
                "local count = 0 " +
                        "local score = ARGV[1] " +
                        "local convId = ARGV[2] " +
                        "for i, key_name in ipairs(KEYS) do " +
                        "  if redis.call('EXISTS', key_name) == 1 then " +
                        "    redis.call('ZADD', key_name, score, convId) " +
                        "    count = count + 1 " +
                        "  end " +
                        "end " +
                        "return count";
        BATCH_ZADD_IF_EXISTS_SCRIPT = new DefaultRedisScript<>(script, Long.class);
    }

    @RabbitListener(queues = MessageQueueConstant.Queue_Contact_Conversation)
    public void consumeContactConversationQueue(ContactConversationEvent event){
        switch (event.getContactConversationType()){
            // 1:好友申请
            case CONTACT_APPLY -> {
                // 1. 删除缓存
                contactApplyCacheService.deleteContactApplyCache(event.getReceiverId());
                // 2. 发送WS通知
                ContactApplyNotification notification = ContactApplyNotification.builder()
                        .senderId(event.getSenderId())
                        .description(event.getDescription())
                        .timestamp(event.getTimestamp())
                        .build();
                messageService.sendContactApplyNotification(event.getReceiverId(), notification);
            }
            // 2:同意好友申请
            case APPLY_AGREE -> {
                ContactCreateWsNotification wsNotification = new ContactCreateWsNotification();
                wsNotification.setConversationVO(event.getConversationVO());
                wsNotification.setUserId(event.getSenderId());
                wsNotification.setTimestamp(event.getTimestamp());

                messageService.sendContactCreateNotification(event.getReceiverId(), wsNotification);
            }
            // 3:拒绝好友申请
            case APPLY_REFUSE -> {

            }
            // 4:删除好友
            case CONTACT_DELETE -> {

            }
            // 5:创建群聊
            case GROUP_CREATE -> {
                // 1. 先批量更新群成员会话列表
                log.info("开始批量更新群成员会话列表, 群组ID: {}", event.getConversationVO().getId());
                // 1.1 构建 KEYS (所有成员的 ZSet Key)
                List<String> keys = event.getAllReceiverIds().stream()
                        .map(id -> RedisConstant.USER_CONVERSATIONS_ZSET + id)
                        .collect(Collectors.toList());

                // 1.2 一次性执行 Lua 脚本 (非 Pipelined 模式，因为 Lua 本身就是原子且批量的)
                // 参数：Score (时间戳), MemberValue (会话ID)
                Long updatedCount = stringRedisTemplate.execute(
                        BATCH_ZADD_IF_EXISTS_SCRIPT,
                        keys,
                        String.valueOf(event.getTimestamp()),
                        String.valueOf(event.getConversationVO().getId())
                );
                log.info("成功更新 {} 名在线用户的 Redis 会话列表", updatedCount);

                // 2. 发送WS通知
                GroupCreateWsNotification wsNotification = new GroupCreateWsNotification();
                wsNotification.setConversationVO(event.getConversationVO());
                wsNotification.setOwnerId(event.getSenderId());
                wsNotification.setTimestamp(event.getTimestamp());

                for (Long memberId : event.getAllReceiverIds()) {
                    messageService.sendGroupCreateNotification(memberId, wsNotification);
                }
            }
            // 6:移除群聊
            case GROUP_REMOVE -> {

            }
            // 7:解散群聊
            case GROUP_DISBAND -> {

            }
            // 8:已读回执
            case READ_RECEIPT -> {
                com.telechat.websocket.message.ReadReceiptNotification wsNotification = com.telechat.websocket.message.ReadReceiptNotification.builder()
                        .conversationId(event.getConversationVO().getId())
                        .userId(event.getSenderId())
                        .maxReadSeqId(Long.valueOf(event.getDescription()))
                        .timestamp(event.getTimestamp())
                        .build();

                if (event.getAllReceiverIds() != null && !event.getAllReceiverIds().isEmpty()) {
                    for (Long receiverId : event.getAllReceiverIds()) {
                        messageService.sendReadReceiptNotification(receiverId, wsNotification);
                    }
                } else if (event.getReceiverId() != null) {
                    messageService.sendReadReceiptNotification(event.getReceiverId(), wsNotification);
                }
            }
        }

    }
}
