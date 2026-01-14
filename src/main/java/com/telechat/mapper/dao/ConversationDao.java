/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午9:36
 */
package com.telechat.mapper.dao;

import com.telechat.mapper.ConversationMapper;
import com.telechat.pojo.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConversationDao {
    @Autowired
    private ConversationMapper conversationMapper;
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

}
