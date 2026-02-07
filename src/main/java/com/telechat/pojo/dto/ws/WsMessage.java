package com.telechat.pojo.dto.ws;

import com.telechat.pojo.enums.WsMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WsMessage<T> {
    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息唯一ID (关键新增)
     * 用于：去重、ACK确认、前端拉取详情
     * 对于"正在输入"等瞬时消息，此字段可为 null
     */
    private Long messageId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 业务数据
     */
    private T data;

    /**
     * 快速构建业务消息
     */
    public static <T> WsMessage<T> of(WsMessageType type, Long messageId, Long senderId, T data) {
        return WsMessage.<T>builder()
                .type(type.getValue())
                .messageId(messageId)
                .senderId(senderId)
                .data(data)
                .build();
    }
}