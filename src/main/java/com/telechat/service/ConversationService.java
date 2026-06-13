package com.telechat.service;

import com.telechat.pojo.vo.ConversationSyncVO;
import com.telechat.pojo.vo.ConversationVO;

import java.util.Collection;
import java.util.List;

public interface ConversationService {

    /**
     * 会话数据列表预热
     *
     * @param userId 用户ID
     * @return true: 预热成功/false: 预热失败
     */
    // Boolean preHeatConversationZSets(Long userId);



    /**
     * 懒加载会话数据，每次设为20条，根据cursor游标后20个数据。如果不够，那么去数据库回源出相应数量的zset数据
     *
     * @param userId 用户ID
     * @param cursor 末尾会话的score（时间戳权重）
     * @return List<ConversationVO>
     */
    // List<ConversationVO> lazyLoadConversations(Long userId, Double cursor);

    ConversationSyncVO syncConversations(Long userId, String deviceId, Long clientLastSyncTime);


    /**
     * 获取单个会话信息
     *
     * @param userId 用户ID
     * @param conversationId 会话ID
     * @return ConversationVO
     */
    ConversationVO getConversationInfo(Long userId, Long conversationId);

    /**
     * 创建群聊
     *
     * @param userId 用户ID
     * @param memberIds 群聊成员IDs
     * @return ConversationVO 创建的群聊返回视图
     */
    ConversationVO createGroup(Long userId, Collection<Long> memberIds);

    /**
     * 置顶/取消置顶会话
     */
    void topConversation(Long userId, Long conversationId, Boolean isTop);

    /**
     * 会话免打扰
     */
    void muteConversation(Long userId, Long conversationId, Boolean isMuted);

    /**
     * 删除/隐藏会话
     */
    void deleteConversation(Long userId, Long conversationId);

    /**
     * 退出群聊
     */
    void exitGroup(Long userId, Long conversationId);

    /**
     * 解散群聊 (仅群主)
     */
    void disbandGroup(Long userId, Long conversationId);
}
