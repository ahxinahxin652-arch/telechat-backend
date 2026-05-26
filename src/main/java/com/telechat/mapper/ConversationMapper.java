package com.telechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.telechat.cache.entity.ConversationZSetCache;
import com.telechat.pojo.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    /**
     *
     * 会话缓存预热：只返回关系
     * param userId
     * @return List<ConversationMemberVO>
     * */
    List<ConversationZSetCache> selectConversationIdsByUserId(@Param("userId") Long userId, @Param("preHeatCount") Integer preHeatCount);


    /**
     *
     * 会话懒加载补充
     * param userId
     * @return List<ConversationMemberVO>
     * */
    List<ConversationZSetCache> selectOlderConversations(
            @Param("userId") Long userId,
            @Param("lastIsToped") boolean lastIsToped,
            @Param("lastTime") LocalDateTime lastTime,
            @Param("limit") int limit
    );
}
