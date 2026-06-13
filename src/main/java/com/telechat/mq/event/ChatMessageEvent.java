package com.telechat.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import lombok.NoArgsConstructor;
import com.telechat.pojo.entity.ChatMessage;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long senderId;
    private Long conversationId;
    private ChatMessage message;
    private java.util.List<Long> receiverIds;
}
