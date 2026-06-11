package com.telechat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("chat_message")
public class ChatMessage {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long conversationId;
    private Long seqId;
    private Long senderId;
    private String content;
    private Integer messageType;
    private LocalDateTime createdTime;
    private Byte status;
}