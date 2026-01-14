package com.telechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.telechat.pojo.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
