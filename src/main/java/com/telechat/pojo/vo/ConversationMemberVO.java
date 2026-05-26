package com.telechat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话成员试图
 * 只返回用户所用户的会话关系，即只需要查询会话id与会话成员状态即可，会话信息是共享的，不需要单独为一个用户查
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMemberVO {
    // 会话Id
    private Long conversationId;
    private boolean isMuted;
    private boolean isToped;
}
