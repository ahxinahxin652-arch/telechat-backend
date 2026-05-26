package com.telechat.websocket.message;

import com.telechat.pojo.vo.ConversationVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactCreateWsNotification {
    // 同意申请的用户ID
    private Long userId;

    // 私聊信息
    private ConversationVO conversationVO;

    // 时间
    private Long timestamp;
}
