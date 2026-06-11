package com.telechat.controller.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telechat.pojo.dto.conversation.SendMessageDTO;
import com.telechat.pojo.entity.ChatMessage;
import com.telechat.pojo.result.Result;
import com.telechat.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/chat")
@Tag(name = "聊天接口")
@Slf4j
public class ChatMessageController {

    @Resource
    private ChatMessageService chatMessageService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@RequestBody SendMessageDTO dto) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ChatMessage message = chatMessageService.sendMessage(
                userId,
                dto.getConversationId(),
                dto.getContent(),
                dto.getMessageType() != null ? dto.getMessageType() : 1
        );
        return Result.success(message);
    }

    @Operation(summary = "更新会话的已读游标")
    @PutMapping("/read/{conversationId}")
    public Result<Void> markMessageAsRead(
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(value = "seqId", required = false, defaultValue = "0") Long seqId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatMessageService.markMessageAsRead(userId, conversationId, seqId);
        return Result.success();
    }
}
