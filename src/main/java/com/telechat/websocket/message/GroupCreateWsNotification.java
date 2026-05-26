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
public class GroupCreateWsNotification {
    // 创建者Id
    private Long ownerId;

    // 群聊信息
    private ConversationVO conversationVO;

    // 时间戳
    private Long timestamp;
}
