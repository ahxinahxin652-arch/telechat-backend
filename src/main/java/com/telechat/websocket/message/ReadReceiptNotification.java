package com.telechat.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadReceiptNotification {
    private Long conversationId;
    private Long userId; // Who read the messages
    private Long maxReadSeqId; // The max seq_id read
    private Long timestamp;
}
