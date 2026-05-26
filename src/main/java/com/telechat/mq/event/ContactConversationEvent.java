package com.telechat.mq.event;

import com.telechat.pojo.enums.mq.ContactConversationType;
import com.telechat.pojo.vo.ConversationVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactConversationEvent {
    // 类型: 1:contact_apply, 2:apply_agree, 3:apply_refuse, 4:contact_delete, 5:group_create, 6:group_remove, 7:group_disband
    private ContactConversationType contactConversationType;

    // 发件人
    private Long senderId;

    // 收件人ID
    private Long receiverId;

    // 收件人IDs
    private Collection<Long> allReceiverIds;

    // 会话视图
    private ConversationVO conversationVO;

    // 描述
    private String description;

    // 时间戳
    private long timestamp;
}
