package com.telechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.telechat.pojo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    
    List<ChatMessage> selectMessagesAfterTime(@Param("conversationIds") List<Long> conversationIds, @Param("syncTime") LocalDateTime syncTime);
}
