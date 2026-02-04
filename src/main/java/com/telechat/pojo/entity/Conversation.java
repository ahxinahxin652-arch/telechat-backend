/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午12:30
 */
package com.telechat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.telechat.pojo.enums.ConversationStatus;
import com.telechat.pojo.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

@TableName("conversation")
public class Conversation {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private ConversationType type;
    private String title;
    private String avatar;
    private Long ownerId;
    private ConversationStatus status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    private Long LastMessageId;
    private String LastMessageContent;
    private LocalDateTime LastMessageTime;
}
