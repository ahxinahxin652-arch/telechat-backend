package com.telechat.controller.user;

import com.telechat.pojo.dto.conversation.CreateGroupDTO;
import com.telechat.pojo.result.Result;
import com.telechat.pojo.vo.ConversationSyncVO;
import com.telechat.pojo.vo.ConversationVO;
import com.telechat.service.ConversationService;
import com.telechat.util.RedisTemplateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
@Tag(name = "会话接口")
@Slf4j
public class ConversationController {
    // service
    @Resource
    private ConversationService conversationService;

    /**
     * 预热会话数据
     * param userId
     * @return boolean
     */
    // @Operation(summary = "预热会话列表数据ZSet")
    // @GetMapping("/preHeat")
    // public Result<Boolean> preHeatConversations() {
    //     // 从Security上下文中获取用户ID
    //     Long userId = (Long) SecurityContextHolder.getContext()
    //             .getAuthentication().getPrincipal();
    //
    //     return Result.success(conversationService.preHeatConversationZSets(userId));
    // }

    /**
     * 懒加载会话
     * param cursor 游标
     * @return boolean
     */
    // @Operation(summary = "懒加载会话")
    // @GetMapping("/lazyLoad")
    // public Result<List<ConversationVO>> lazyLoadConversations(Double cursor) {
    //     // 从Security上下文中获取用户ID
    //     Long userId = (Long) SecurityContextHolder.getContext()
    //             .getAuthentication().getPrincipal();
    //
    //     List<ConversationVO> conversationVOS = conversationService.lazyLoadConversations(userId, cursor);
    //     return Result.success(conversationVOS);
    // }

    @Operation(summary = "增量同步拉取会话列表及离线消息")
    @GetMapping("/sync")
    public Result<ConversationSyncVO> syncConversations(@RequestParam("lastSyncTime") Long lastSyncTime) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ConversationSyncVO syncVO = conversationService.syncConversations(userId, lastSyncTime);
        return Result.success(syncVO);
    }

    /**
     * 创建群聊会话
     * param cursor 游标
     * @return boolean
     */
    @Operation(summary = "创建群聊会话")
    @PostMapping("/createGroup")
    public Result<ConversationVO> createGroup(@RequestBody CreateGroupDTO createGroupDTO) {
        // 从Security上下文中获取用户ID
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        ConversationVO conversationVO =  conversationService.createGroup(userId, createGroupDTO.getMemberIds());
        return Result.success(conversationVO);
    }

    /**
     * 获取单个群聊信息
     * param
     * @return 群聊信息
     */
    @Operation(summary = "获取单个群聊信息")
    @GetMapping("/{id}")
    public Result<ConversationVO> getConversationInfo(@PathVariable("id") Long conversationId) {
        // 从Security上下文中获取用户ID
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        ConversationVO conversationVO = conversationService.getConversationInfo(userId ,conversationId);
        return Result.success(conversationVO);
    }

    @Operation(summary = "置顶/取消置顶会话")
    @PutMapping("/{id}/top")
    public Result<Void> topConversation(@PathVariable("id") Long conversationId, @RequestParam("isTop") Boolean isTop) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        conversationService.topConversation(userId, conversationId, isTop);
        return Result.success();
    }

    @Operation(summary = "设置会话免打扰")
    @PutMapping("/{id}/mute")
    public Result<Void> muteConversation(@PathVariable("id") Long conversationId, @RequestParam("isMuted") Boolean isMuted) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        conversationService.muteConversation(userId, conversationId, isMuted);
        return Result.success();
    }

    @Operation(summary = "隐藏/删除会话")
    @DeleteMapping("/{id}")
    public Result<Void> deleteConversation(@PathVariable("id") Long conversationId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        conversationService.deleteConversation(userId, conversationId);
        return Result.success();
    }

    @Operation(summary = "退出群聊")
    @DeleteMapping("/{id}/exit")
    public Result<Void> exitGroup(@PathVariable("id") Long conversationId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        conversationService.exitGroup(userId, conversationId);
        return Result.success();
    }

    @Operation(summary = "解散群聊")
    @DeleteMapping("/{id}/disband")
    public Result<Void> disbandGroup(@PathVariable("id") Long conversationId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        conversationService.disbandGroup(userId, conversationId);
        return Result.success();
    }
}
