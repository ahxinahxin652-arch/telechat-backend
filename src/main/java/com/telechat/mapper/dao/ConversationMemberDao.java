/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午10:01
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.telechat.mapper.ConversationMemberMapper;
import com.telechat.pojo.entity.ConversationMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConversationMemberDao {
    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    /**
     * 插入会话成员
     *
     * @param conversationMember 会话成员
     */
    public void insert(ConversationMember conversationMember) {
        conversationMemberMapper.insert(conversationMember);
    }

    /**
     * 根据会话ID和用户ID查询会话成员
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @return ConversationMember
     */
    public ConversationMember selectByConversationIdAndUserId(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId);
        return conversationMemberMapper.selectOne(queryWrapper);
    }

    /**
     * 根据会话ID更新会话成员
     *
     * @param conversationMember 会话
     */
    public void updateById(ConversationMember conversationMember) {
        conversationMemberMapper.updateById(conversationMember);
    }
}
