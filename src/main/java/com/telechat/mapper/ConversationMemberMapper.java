package com.telechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.telechat.pojo.entity.ConversationMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMemberMapper extends BaseMapper<ConversationMember> {
    /**
     * 批量插入
     * @param memberList 成员列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<ConversationMember> memberList);
}
