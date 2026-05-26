package com.telechat.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.telechat.pojo.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表视图对象
 * 聚合了 Conversation 和 ConversationMember 的数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationVO {
    // 会话ID
    private Long id;
    // 会话类型
    private ConversationType type;
    // 展示名称（私聊为对方昵称/备注，群聊为群名）
    private String title;
    // 展示头像（私聊为对方头像，群聊为群头像）
    private String avatar;
    // 未读消息数（计算得出）
    private Integer unreadCount;
    // 是否置顶
    private Boolean isTop;
    // 是否免打扰
    private Boolean isMuted;
    // 最新消息内容（用于列表预览）
    private String lastMessageContent;
    // 最新消息时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMessageTime;
    // 关键参数：给前端下一次分页请求使用的游标
    private Double score;
}