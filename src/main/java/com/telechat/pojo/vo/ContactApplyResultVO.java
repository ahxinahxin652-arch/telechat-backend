package com.telechat.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactApplyResultVO {
    
    /**
     * 申请结果状态：
     * 0 - 申请已发送，等待对方验证
     * 1 - 对方已向您发送过申请，已直接互相添加为好友
     */
    private Integer status;
    
    /**
     * 好友会话视图。仅当 status = 1 时，该字段才有值
     */
    private ConversationVO conversationVO;
}