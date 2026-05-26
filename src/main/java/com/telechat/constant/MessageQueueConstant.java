package com.telechat.constant;

public class MessageQueueConstant {
    /**
     * 交换机 (使用 Topic 交换机，路由极其灵活)
     */
    // 死信交换机
    public static final String ErrorDirectExchange = "exchange.telechat.error";
    // 业务交换机
    public static final String EventTopicExchange = "exchange.telechat.event";

    // ----------------- 路由键 (Routing Key) -----------------
    public static final String RK_ERROR = "error";
    public static final String RK_GROUP_CREATE = "group.create";
    public static final String RK_PRIVATE_CREATE = "private.create";
    public static final String RK_CONTACT_APPLY = "contact.apply";
    public static final String RK_CHAT_MESSAGE = "chat.message";
    public static final String RK_CONTACT_CONVERSATION = "contact.conversation";

    // ----------------- 消息队列 (Queues) -----------------
    // 死信队列
    public static final String Queue_Error = "queue.error";

    // 专门负责处理【私聊创建】的 WebSocket 推送逻辑
    public static final String Queue_ContactCreate_WS = "queue.contact.create.ws";

    // 专门负责处理【群聊创建】的 Redis 更新逻辑
    public static final String Queue_GroupCreate_Redis = "queue.group.create.redis";

    // 专门负责处理【群聊创建】的 WebSocket 推送逻辑
    public static final String Queue_GroupCreate_WS = "queue.group.create.ws";

    // 聊天消息队列
    public static final String Queue_Chat_Message = "queue.chat.message";
    // 好友与会话队列
    public static final String Queue_Contact_Conversation = "queue.contact.conversation";
    // 用户状态与信令
    // 系统通知
}
