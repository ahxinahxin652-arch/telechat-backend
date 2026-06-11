package com.telechat.pojo.vo;

import com.telechat.pojo.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSyncVO {
    // 离线期间的增量消息字典 (Key: conversationId, Value: ChatMessage list)
    private Map<Long, List<ChatMessage>> messages;

    // 离线期间更新/新增的会话元数据（包含离线新群/新好友的完整信息）
    private List<ConversationVO> conversations;
}
