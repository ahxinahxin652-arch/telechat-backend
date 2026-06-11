/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午12:31
 */
package com.telechat.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.telechat.pojo.enums.ConversationMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

@TableName("conversation_member")
public class ConversationMember {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private Long conversationId;
    private Long userId;
    private ConversationMemberRole role;
    @TableField("is_muted")
    private boolean isMuted;
    @TableField("is_deleted")
    private boolean isDeleted;
    @TableField("is_toped")
    private boolean isToped;
    private Long lastReadMessageId;
    private LocalDateTime joinedTime;
    private LocalDateTime updatedTime;
}
