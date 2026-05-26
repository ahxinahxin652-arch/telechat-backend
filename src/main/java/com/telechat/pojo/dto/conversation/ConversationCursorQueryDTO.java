package com.telechat.pojo.dto.conversation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ConversationCursorQueryDTO {
    // 游标，即上一页最后一条的 score。第一页传 null
    private Double cursor;
    
    // 每页大小，限制最大值防止恶意拉取
    @Min(1)
    @Max(50)
    private Integer limit = 20;
}