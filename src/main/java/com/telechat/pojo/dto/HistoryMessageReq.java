package com.telechat.pojo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class HistoryMessageReq {
    @NotNull(message = "conversationId不能为空")
    private Long conversationId;

    @NotNull(message = "anchorSeqId不能为空")
    private Long anchorSeqId;

    // 方向：older (向下查历史), newer (向上查新消息), around (前后都查)
    private String direction = "older";

    private Integer limit = 30;
}
