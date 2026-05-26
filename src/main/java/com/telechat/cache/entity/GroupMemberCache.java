package com.telechat.cache.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.telechat.pojo.enums.ConversationMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupMemberCache {
    // conversation_member id
    private Long id;

    // 角色
    private ConversationMemberRole role;

    // 防止缓存穿透的标记方法
    @JsonIgnore
    public boolean isNullPlaceholder() {
        return this.id != null && this.id == -1L;
    }
}
