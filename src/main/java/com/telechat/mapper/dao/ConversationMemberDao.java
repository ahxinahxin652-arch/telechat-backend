/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午10:01
 */
package com.telechat.mapper.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

    /**
     * 更新会话成员的通用设置 (免打扰、角色等低频操作)
     */
    public int updateSettings(ConversationMember member) {
        // 这里使用 QueryWrapper 只做 WHERE 条件的拼接
        LambdaQueryWrapper<ConversationMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMember::getConversationId, member.getConversationId())
                .eq(member.getUserId() != null, ConversationMember::getUserId, member.getUserId());

        // 将 member 传进去，MyBatis-Plus 底层会自动判断：
        // 如果 member.getRole() != null，就会自动在 SQL 里加上 role = xxx
        // 不需要手写任何 .set()，代码极其清爽
        return conversationMemberMapper.update(member, queryWrapper);
    }

    /**
     * 更新用户在某个会话中的最后阅读消息ID (高频操作)
     */
    public int updateLastReadMessageId(Long conversationId, Long userId, Long lastReadMessageId) {
        LambdaUpdateWrapper<ConversationMember> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId)
                // 直接 set 目标字段
                .set(ConversationMember::getLastReadMessageId, lastReadMessageId);

        // 注意这里第一个参数传 null，完全交给 UpdateWrapper 处理
        return conversationMemberMapper.update(null, updateWrapper);
    }

    /**
     * 删除会话成员
     */
    public int delete(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMember::getConversationId, conversationId)
                .eq(ConversationMember::getUserId, userId);

        return conversationMemberMapper.delete(queryWrapper);
    }
}
