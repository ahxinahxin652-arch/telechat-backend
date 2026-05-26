/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午12:41
 */
package com.telechat.controller.user;

import com.telechat.pojo.dto.contact.AddContactApplyDTO;
import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
import com.telechat.pojo.result.Result;
import com.telechat.pojo.vo.ContactApplyResultVO;
import com.telechat.pojo.vo.ContactApplyVO;
import com.telechat.pojo.vo.ContactApplyVO;
import com.telechat.pojo.vo.ConversationVO;
import com.telechat.service.ContactApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contactApply")
@Tag(name = "联系申请接口")
@Slf4j
public class ContactApplyController {
    // service
    @Resource
    private ContactApplyService contactApplyService;

    @Operation(summary = "添加联系人")
    @PostMapping("/add")
    public Result<ContactApplyResultVO> add(@RequestBody AddContactApplyDTO addContactApplyDTO) {
        // 添加联系人
        // 1.获取添加的用户名
        String username = addContactApplyDTO.getUserName();
        log.info("添加联系人: {}", username);

        // 2.获取当前用户ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 3.添加联系人
        ContactApplyResultVO contactApplyResultVO = contactApplyService.addContactApply(userId, username);

        return Result.success(contactApplyResultVO);
    }

    @Operation(summary = "获取联系人申请列表【未处理】")
    @GetMapping("/apply/list")
    public Result<List<ContactApplyVO>> applyList() {
        //  获取用户id
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("获取联系人申请列表: {}", userId);
        // 获取联系人申请列表
        List<ContactApplyVO> applyList = contactApplyService.applyList(userId);
        return Result.success(applyList);
    }

    @Operation(summary = "处理联系人申请")
    @PostMapping("/apply/handle")
    public Result<ConversationVO> handleApply(@RequestBody ContactApplyHandleDTO contactApplyHandleDTO) {
        // 处理联系人申请
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("处理联系人申请: {}", contactApplyHandleDTO);
        ConversationVO conversationVO = contactApplyService.handleApply(userId, contactApplyHandleDTO);
        if (conversationVO != null) {
            return Result.success(conversationVO);
        }
        else {
            return Result.success(null);
        }
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读好友申请数量")
    public Result<Long> getUnreadCount() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(contactApplyService.getUnreadCount(userId));
    }

    @PutMapping("/read-all")
    @Operation(summary = "清空好友申请未读状态")
    public Result<String> markAllAsRead() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        contactApplyService.markAllAsRead(userId);
        return Result.success();
    }
}
