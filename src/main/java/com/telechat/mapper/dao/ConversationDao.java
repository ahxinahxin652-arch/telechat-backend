/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午9:36
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.telechat.mapper.ConversationMapper;
import com.telechat.cache.entity.ConversationZSetCache;
import com.telechat.pojo.entity.Conversation;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
public class ConversationDao {
    @Resource
    private ConversationMapper conversationMapper;

    /**
     * 会话预热查询：只返回关系，联表查询，使用xml性能更高
     * param userId
     *
     * @return List<ConversationMemberVO>
     */
    public List<ConversationZSetCache> selectConversationIdsByUserId(Long userId, Integer preHeatCount) {
        return conversationMapper.selectConversationIdsByUserId(userId, preHeatCount);
    }

    /**
     * 会话懒加载补充
     * param userId
     *
     * @return List<ConversationMemberVO>
     */
    public List<ConversationZSetCache> selectOlderConversations(Long userId, boolean lastIsToped,
                                                         LocalDateTime lastTime, int limit
    ) {
        return conversationMapper.selectOlderConversations(userId, lastIsToped, lastTime, limit);
    }

    /**
     * 插入会话
     *
     * @param conversation 会话
     */
    public void insert(Conversation conversation) {
        conversationMapper.insert(conversation);
    }

    /**
     * 根据会话ID查询会话
     *
     * @param conversationId 会话ID
     * @return Conversation
     */
    public Conversation selectById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /**
     * 根据会话唯一标识查询会话
     *
     * @param uniqueKey 会话标识
     * @return Conversation
     */
    public Conversation selectByUniqueKey(String uniqueKey) {
        LambdaQueryWrapper<Conversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Conversation::getUniqueKey, uniqueKey);
        return conversationMapper.selectOne(queryWrapper);
    }

    /**
     * 根据ID集合批量查询会话
     * 对应 SQL: SELECT * FROM conversation WHERE id IN (1, 2, 3...)
     *
     * @param conversationIds 会话ID集合
     * @return 会话列表
     */
    public List<Conversation> selectBatchIds(List<Long> conversationIds) {
        // 判空处理，防止传入空集合导致MybatisPlus报错或无效查询
        if (conversationIds == null || conversationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return conversationMapper.selectBatchIds(conversationIds);
    }
}
