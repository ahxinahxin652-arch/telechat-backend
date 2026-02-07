/**
 * 功能
 * 作者: 那不勒斯的老大
 * 日期: 2025/12/21 下午12:41
 */
package com.telechat.controller.user;

import com.telechat.pojo.dto.contact.AddContactApplyDTO;
import com.telechat.pojo.dto.contact.ContactApplyHandleDTO;
import com.telechat.pojo.result.Result;
import com.telechat.pojo.vo.ContactApplyVO;
import com.telechat.service.ContactApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contactApply")
@Tag(name = "联系申请接口")
@Slf4j
public class ContactApplyController {

    @Autowired
    private ContactApplyService contactApplyService;

    @Operation(summary = "添加联系人")
    @PostMapping("/add")
    public Result<String> add(@RequestBody AddContactApplyDTO addContactApplyDTO) {
        // 添加联系人
        // 1.获取添加的用户名
        String username = addContactApplyDTO.getUserName();
        log.info("添加联系人: {}", username);

        // 2.获取当前用户ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 3.添加联系人
        boolean is_added = contactApplyService.addContactApply(userId, username);

        if (is_added) {
            return Result.success("发送添加联系人申请成功");
        } else {
            return Result.error(500, "发送添加联系人申请失败");
        }
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
    public Result<String> handleApply(@RequestBody ContactApplyHandleDTO contactApplyHandleDTO) {
        // 处理联系人申请
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("处理联系人申请: {}", contactApplyHandleDTO);
        boolean is_handle = contactApplyService.handleApply(userId, contactApplyHandleDTO);
        if (is_handle && contactApplyHandleDTO.isAgree()) {
            return Result.success("同意成功");
        }
        else if (is_handle) {
            return Result.success("拒绝成功");
        }
        else {
            return Result.error(500, "处理联系人申请失败");
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
