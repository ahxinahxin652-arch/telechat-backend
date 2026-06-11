package com.telechat.pojo.dto.conversation;

import lombok.Data;

@Data
public class SendMessageDTO {
    private Long conversationId;
    private String content;
    private Integer messageType; // e.g. 1 for TEXT, 2 for IMAGE
}
