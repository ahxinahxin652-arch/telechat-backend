/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/30 下午8:57
 */
package com.telechat.constant;

public class RedisConstant {
    public static final String USER_TOKEN = "user:token:";

    // 空信息，防止缓存穿透
    public static final Long EMPTY_DATA = 5L;

    // 锁
    public static final String LOCK_CONTACT_APPLY = "lock:contactApply:";
    public static final Long LOCK_CONTACT_APPLY_DURATION = 5L;

    public static final String USER_CONVERSATIONS_PREHEAT_LOCK = "lock:userConversations:preheatLock:";

    // 用户信息缓存
    public static final String USER_INFO = "user:info:";
    public static final Long USER_INFO_DURATION = 30L; //30min

    // 用户联系人信息缓存
    public static final String USER_CONTACTS_INFO = "user:contacts:";
    public static final Long USER_CONTACTS_INFO_DURATION = 30L; //30min

    // 用户联系人申请信息缓存
    public static final String USER_CONTACTS_APPLY = "user:contactApplies:";
    public static final Long USER_CONTACT_APPLIES_DURATION = 60L;

    // --- 会话模块常量 ---

    // 用户会话列表 ZSet (Score = 时间戳 + 置顶权重)
    public static final String USER_CONVERSATIONS_ZSET = "user:conversations:zset:";
    public static final Long USER_CONVERSATIONS_ZSET_DURATION = 24L;  // 24h

    // 用户对具体会话的私有状态 Hash (未读数, 免打扰等) -> Field: conversationId
    public static final String USER_CONVERSATION_MEMBER = "user:conversationMember:";
    public static final Long USER_CONVERSATION_MEMBER_DURATION = 60L;

    // 会话的全局静态信息缓存 String (JSON)
    public static final String CONVERSATION_STATIC_INFO = "conversation:static:info:";
    public static final Long CONVERSATION_STATIC_INFO_DURATION = 1200L; // 1200min

    // 会话的全局动态信息缓存 Hash
    public static final String CONVERSATION_META_INFO = "conversation:meta:info:";
    public static final Long CONVERSATION_META_INFO_DURATION = 5L; // 5min，这个变化频繁，持续时间设短一点

    // 隐藏的会话 ZSet (暂存区)
    public static final String USER_CONVERSATIONS_HIDDEN = "user:conversations:hidden:";

    // 群成员信息缓存 Set
    public static final String CONVERSATION_GROUP_MEMBER = "conversation:groupMember:";
    public static final Long CONVERSATION_GROUP_MEMBER_DURATION = 3L; // 3d
    
    // 会话消息序列号 (INCR)
    public static final String CONVERSATION_SEQ_ID = "conversation:seq:";
}
