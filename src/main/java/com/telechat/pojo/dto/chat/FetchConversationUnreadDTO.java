package com.telechat.pojo.dto.chat;

import lombok.Data;

import java.util.List;

@Data
public class FetchConversationUnreadDTO {
    // 需要查询的会话ID集合
    private List<Long> conversationIds;
}
